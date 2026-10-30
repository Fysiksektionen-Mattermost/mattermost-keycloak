package se.fysiksektionen.keycloak;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public final class MattermostIntroductoryEmailEventListener implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(MattermostIntroductoryEmailEventListener.class);

    public static final String DEFAULT_REALM = "mattermost";
    public static final String DEFAULT_CLIENT_ID = "mattermost";
    public static final String DEFAULT_ATTRIBUTE_SENT = "introductory-email-sent";

    private static final String DEFAULT_SUBJECT = "Welcome to Mattermost";

    private static final String DEFAULT_TEXT_BODY = """
            Hi {firstName},

            Welcome to Mattermost! You have completed your first login.

            Your account details:
            - Username: {mattermostUsername}
            - Email: {email}

            You can access Mattermost anytime at:
            https://mattermost.fysiksektionen.se/

            If you have any questions or need assistance, feel free to reach out to the system administrators.

            Best regards,
            Fysiksektionen
            """;

    private static final String DEFAULT_HTML_BODY = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Welcome to Mattermost</title>
              <style>
                body {
                  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                  margin: 0;
                  padding: 24px;
                  background-color: #f4f6f8;
                  color: #333333;
                  line-height: 1.6;
                }
                .container {
                  max-width: 580px;
                  margin: 0 auto;
                  background: #ffffff;
                  border-radius: 8px;
                  overflow: hidden;
                  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
                }
                .header {
                  background-color: #1e3a8a;
                  color: #ffffff;
                  padding: 28px 24px;
                  text-align: center;
                }
                .header h1 {
                  margin: 0;
                  font-size: 22px;
                  font-weight: 600;
                  letter-spacing: -0.5px;
                }
                .content {
                  padding: 32px 28px;
                }
                .info-box {
                  background-color: #f8fafc;
                  border: 1px solid #e2e8f0;
                  border-left: 4px solid #1e3a8a;
                  border-radius: 4px;
                  padding: 16px 20px;
                  margin: 24px 0;
                }
                .info-box p {
                  margin: 4px 0;
                  font-size: 14px;
                }
                .btn-container {
                  text-align: center;
                  margin: 32px 0 16px 0;
                }
                .btn {
                  display: inline-block;
                  background-color: #1e3a8a;
                  color: #ffffff !important;
                  text-decoration: none;
                  font-weight: 600;
                  font-size: 15px;
                  padding: 12px 28px;
                  border-radius: 6px;
                }
                .footer {
                  background-color: #f8fafc;
                  border-top: 1px solid #e2e8f0;
                  padding: 20px 24px;
                  text-align: center;
                  font-size: 12px;
                  color: #64748b;
                }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Welcome to Mattermost</h1>
                </div>
                <div class="content">
                  <p>Hi {firstName},</p>
                  <p>Welcome to <strong>Mattermost</strong>! You have successfully completed your first login.</p>
                  <div class="info-box">
                    <p><strong>Username:</strong> {mattermostUsername}</p>
                    <p><strong>Email:</strong> {email}</p>
                  </div>
                  <p>You can now join channels, collaborate with team members, and stay up to date with announcements.</p>
                  <div class="btn-container">
                    <a href="https://mattermost.fysiksektionen.se/" class="btn">Open Mattermost</a>
                  </div>
                </div>
                <div class="footer">
                  <p style="margin: 0;">This email was sent automatically by Fysiksektionen Keycloak.</p>
                </div>
              </div>
            </body>
            </html>
            """;

    private final KeycloakSession session;

    public MattermostIntroductoryEmailEventListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.LOGIN) {
            return;
        }

        String targetClientId = getTargetClientId();
        if (!targetClientId.equals(event.getClientId())) {
            return;
        }

        if (event.getUserId() == null) {
            return;
        }

        RealmModel realm = session.realms().getRealm(event.getRealmId());
        if (realm == null) {
            return;
        }

        String targetRealm = getTargetRealm();
        if (!targetRealm.equalsIgnoreCase(realm.getName())) {
            return;
        }

        UserModel user = session.users().getUserById(realm, event.getUserId());
        if (user == null) {
            return;
        }

        String attributeName = getAttributeName();
        if (isAlreadySent(user, attributeName)) {
            return;
        }

        String email = user.getEmail();
        if (!hasText(email)) {
            logger.warnf("User %s completed authentication to client %s, but has no email address configured; introductory email skipped.",
                    user.getUsername(), targetClientId);
            return;
        }

        sendIntroductoryEmail(realm, user, attributeName, targetClientId);
    }

    private void sendIntroductoryEmail(RealmModel realm, UserModel user, String attributeName, String clientId) {
        Map<String, String> smtpConfig = realm.getSmtpConfig();
        if (smtpConfig == null || smtpConfig.isEmpty() || !hasText(smtpConfig.get("host"))) {
            logger.warnf("SMTP is not configured for realm %s; introductory email cannot be sent to user %s.",
                    realm.getName(), user.getUsername());
            return;
        }

        EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class);
        if (emailSender == null) {
            logger.errorf("No EmailSenderProvider available in session; introductory email cannot be sent to user %s.",
                    user.getUsername());
            return;
        }

        Map<String, String> replacements = buildReplacements(realm, user, clientId);
        String subject = resolveTemplate(getSubject(realm), replacements);
        String textBody = resolveTemplate(getTextBody(realm), replacements);
        String htmlBody = resolveTemplate(getHtmlBody(realm), replacements);

        try {
            emailSender.send(smtpConfig, user, subject, textBody, htmlBody);
            user.setSingleAttribute(attributeName, "true");
            logger.infof("Introductory email sent to user %s (%s) upon first authentication to client %s.",
                    user.getUsername(), user.getEmail(), clientId);
        } catch (EmailException e) {
            logger.errorf(e, "Failed to send introductory email to user %s (%s) for client %s.",
                    user.getUsername(), user.getEmail(), clientId);
        } catch (Exception e) {
            logger.errorf(e, "Unexpected error occurred while sending introductory email to user %s (%s) for client %s.",
                    user.getUsername(), user.getEmail(), clientId);
        }
    }

    private static boolean isAlreadySent(UserModel user, String attributeName) {
        String value = user.getFirstAttribute(attributeName);
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("false");
    }

    private static Map<String, String> buildReplacements(RealmModel realm, UserModel user, String clientId) {
        Map<String, String> replacements = new HashMap<>();
        String username = sanitize(user.getUsername());
        replacements.put("username", username);
        replacements.put("email", sanitize(user.getEmail()));

        String firstName = user.getFirstName();
        replacements.put("firstName", hasText(firstName) ? firstName : "there");

        String lastName = sanitize(user.getLastName());
        replacements.put("lastName", lastName);

        String fullName = (hasText(firstName) ? firstName : "") +
                (hasText(lastName) ? (hasText(firstName) ? " " : "") + lastName : "");
        replacements.put("fullName", hasText(fullName) ? fullName : username);

        String mmUsername = user.getFirstAttribute("mattermostUsername");
        replacements.put("mattermostUsername", hasText(mmUsername) ? mmUsername : username);

        replacements.put("realm", realm.getName());
        replacements.put("clientId", clientId);
        return replacements;
    }

    private static String resolveTemplate(String template, Map<String, String> replacements) {
        if (template == null) {
            return "";
        }
        String result = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue())
                           .replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private String getSubject(RealmModel realm) {
        String realmAttr = realm.getAttribute("introductoryEmailSubject");
        if (hasText(realmAttr)) {
            return realmAttr;
        }
        String env = System.getenv("MATTERMOST_INTRO_EMAIL_SUBJECT");
        if (hasText(env)) {
            return env;
        }
        return DEFAULT_SUBJECT;
    }

    private String getTextBody(RealmModel realm) {
        String realmAttr = realm.getAttribute("introductoryEmailBodyText");
        if (hasText(realmAttr)) {
            return realmAttr;
        }
        String envFile = System.getenv("MATTERMOST_INTRO_EMAIL_TEXT_FILE");
        if (hasText(envFile)) {
            String fromFile = readFile(envFile);
            if (hasText(fromFile)) {
                return fromFile;
            }
        }
        String env = System.getenv("MATTERMOST_INTRO_EMAIL_TEXT");
        if (hasText(env)) {
            return env;
        }
        return DEFAULT_TEXT_BODY;
    }

    private String getHtmlBody(RealmModel realm) {
        String realmAttr = realm.getAttribute("introductoryEmailBodyHtml");
        if (hasText(realmAttr)) {
            return realmAttr;
        }
        String envFile = System.getenv("MATTERMOST_INTRO_EMAIL_HTML_FILE");
        if (hasText(envFile)) {
            String fromFile = readFile(envFile);
            if (hasText(fromFile)) {
                return fromFile;
            }
        }
        String env = System.getenv("MATTERMOST_INTRO_EMAIL_HTML");
        if (hasText(env)) {
            return env;
        }
        return DEFAULT_HTML_BODY;
    }

    private static String readFile(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            logger.warnf("Could not read introductory email template from file %s: %s", path, e.getMessage());
            return null;
        }
    }

    private static String getTargetRealm() {
        String env = System.getenv("MATTERMOST_REALM");
        return hasText(env) ? env : DEFAULT_REALM;
    }

    private static String getTargetClientId() {
        String env = System.getenv("MATTERMOST_CLIENT_ID");
        return hasText(env) ? env : DEFAULT_CLIENT_ID;
    }

    private static String getAttributeName() {
        String env = System.getenv("MATTERMOST_INTRO_EMAIL_SENT_ATTR");
        return hasText(env) ? env : DEFAULT_ATTRIBUTE_SENT;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String sanitize(String value) {
        return value != null ? value : "";
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
    }

    @Override
    public void close() {
    }
}

