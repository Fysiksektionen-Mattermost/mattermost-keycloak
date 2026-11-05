package se.fysiksektionen.keycloak;

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

    private static final String SUBJECT_KEY = "mattermostIntroductoryEmailSubject";
    private static final String TEXT_BODY_KEY = "mattermostIntroductoryEmailText";
    private static final String HTML_BODY_KEY = "mattermostIntroductoryEmailHtml";
    private static final String DEFAULT_SUBJECT = "Welcome to Mattermost!";

    private static final String DEFAULT_TEXT_BODY = """
            Hi {firstName},

            Welcome to Mattermost! You have completed your first login.

            Your account details:
            - Email: {email}
            - Your password can be updated by clicking on "Forgot password?" on the login page.

            You can access Mattermost anytime at:
            https://mattermost.fysiksektionen.se/ > "International Student"

            We highly recommend that you install the mobile app to easier keep up with the goings-on at Mattermost.
            It is available on Apple App Store and Google Play, search for "Mattermost".
            To log in to the app, type in
            - Server URL: https://mattermost.fysiksektionen.se/
            - Display Name: Fysiksektionen
            - ... and choose to log in as an International Student.

            If you have any questions or need assistance, feel free to reach out to the system administrators at mattermaster@f.kth.se.

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
                  <p>Welcome to <strong>Mattermost</strong>! You have completed your first login.</p>
                  <div class="info-box">
                    <p><strong>Email:</strong> {email}</p>
                    <p>Your password can be updated by clicking on
                      <strong>&quot;Forgot password?&quot;</strong> on the login page.</p>
                  </div>
                  <p>You can access Mattermost anytime at
                    <a href="https://mattermost.fysiksektionen.se/">https://mattermost.fysiksektionen.se/</a>
                    &gt; <strong>&quot;International Student&quot;</strong>.</p>
                  <p>We highly recommend installing the mobile app to make it easier
                    to keep up with what is happening in Mattermost. It is available
                    on the Apple App Store and Google Play; search for
                    <strong>&quot;Mattermost&quot;</strong>.</p>
                  <p>To log in to the app, enter:</p>
                  <ul>
                    <li><strong>Server URL:</strong>
                      <a href="https://mattermost.fysiksektionen.se/">
                        https://mattermost.fysiksektionen.se/</a></li>
                    <li><strong>Display Name:</strong> Fysiksektionen</li>
                    <li>Choose to log in as an <strong>International Student</strong>.</li>
                  </ul>
                  <p>If you have any questions or need assistance, contact the system
                    administrators at <a href="mailto:mattermaster@f.kth.se">
                    mattermaster@f.kth.se</a>.</p>
                </div>
                <div class="footer">
                  <p style="margin: 0;">Best regards,<br>Fysiksektionen</p>
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

        String targetClientId = DEFAULT_CLIENT_ID;
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

        if (!DEFAULT_REALM.equalsIgnoreCase(realm.getName())) {
            return;
        }

        UserModel user = session.users().getUserById(realm, event.getUserId());
        if (user == null) {
            return;
        }

        String attributeName = DEFAULT_ATTRIBUTE_SENT;
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
        Map<String, String> localization = getLocalization(realm, user);
        String subject = resolveTemplate(localization.getOrDefault(SUBJECT_KEY, DEFAULT_SUBJECT), replacements);
        String textBody = resolveTemplate(localization.getOrDefault(TEXT_BODY_KEY, DEFAULT_TEXT_BODY), replacements);
        String htmlBody = resolveTemplate(localization.getOrDefault(HTML_BODY_KEY, DEFAULT_HTML_BODY), replacements);

        try {
            emailSender.send(smtpConfig, user, subject, textBody, htmlBody);
            user.setSingleAttribute(attributeName, "true");
            logger.infof("Introductory email sent to user %s (%s) upon first authentication to client %s.",
                    user.getUsername(), user.getEmail(), clientId);
        } catch (EmailException e) {
            logger.errorf(e, "Failed to send introductory email to user %s (%s) for client %s.",
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

    private static Map<String, String> getLocalization(RealmModel realm, UserModel user) {
        String locale = user.getFirstAttribute(UserModel.LOCALE);
        if (!hasText(locale)) {
            locale = realm.getDefaultLocale();
        }
        return hasText(locale) ? realm.getRealmLocalizationTextsByLocale(locale) : Map.of();
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
