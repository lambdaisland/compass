# Ti.to

From the Ti.to [API Access Tokens](https://id.tito.io/api-access-tokens)
settings page.

- Create API Access Token
  - Test Mode: OFF
  - Type: Secret Key
  - Version: 3.0
  
# Discord

First, make sure you have a Developer Team set up for you conference at the
[Teams Page](https://discord.com/developers/teams). Add `sunnyplexus` as admin.

Then at the [Applications Page](https://discord.com/developers/applications)

- New Application
  - Name: Compass
  - Team: \<your dev team>
  - Create!

Finally in the Discord server settings, give `sunnyplexus` a role which contains
the "Manage Server" permission, in order to add and manage the bot.
  
In the bot settings you'll need to fetch the public key (`:discord/public-key`),
from the OAuth2 tab you need the client-id (`:discord/client-id`) and client
secret (`:discord/client-secret`).

For the OAuth2 redirect URL, set
`https://compass-domain/oauth2/discord/callback`.

On the Installation tab, you can disable "User Install". Under "Guild Install",
add "Scopes: bot", and then "Permissions: Create Instant Invite", "Manage
Roles", "Send Messages", "Send Messages in Threads".

Now you can copy the Install link ("Discord Provided Link"), visit the URL, and
add the bot to the server (guild).

From the "Bot" tab, generate a new token (`discord/bot-token`).

You also need the server-id of the server that the bot will be active in
(`:discord/server-id`). In the regular Discord interface, right click on the
server, "Copy server id" (last entry in the right-click menu).



  
