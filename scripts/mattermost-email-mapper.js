var mattermostEmail = user.getFirstAttribute('mattermostEmail');

if (mattermostEmail !== null && mattermostEmail.trim() !== '') {
  exports = mattermostEmail;
} else {
  exports = user.getEmail();
}
