# Conference Compass

Conference activities and planning app.

- Clojure backend + HTMX
- Datomic
- Discord for Auth
- Integrant
- [Ornament](https://github.com/lambdaisland/ornament) and [Open Props](https://open-props.style/)

This will be used at [Heart of Clojure](https://heartofclojure.eu), replacing
the aging [Eurucamp Activities App](https://github.com/heartofclojure/activities) which we used five years ago
(and which stopped being developed several years before that).

In the Compass app people can find the conference schedule, but also any
additional activities, and they can add their own activities, unconference
style.

People can star/bookmark or sign up for their favorite activities/sessions.
Activity organizers can limit the capacity, so e.g. if you want to take up to 5
people to the climbing gym or go for Thai dinner with up to 8 people you can do
that.

## Discord

We currently only support Discord for authentication. We understand this will
not please everyone, and in the future this may change, but for Heart of Clojure
2024 this will be the only auth option.

The reason is that we also use Discord as the conference chat (backchannel), so
people should have an account there anyway. And this allows us to do some nice
things, like create a private channel with all attendees of a certain activity.

To get started you need to set up a discord bot, and then create a
`config.local.edn` in the project root like this:

```clj
;; config.local.edn
{:discord/client-secret "..."
 :discord/bot-token     "..."
 :discord/client-id     "..."
 :discord/public-key    "..."
 :discord/server-id "..."
 :discord/session-channel-id "..." ;; ID of the channel where session threads will be created
 :discord/ticket-holder-role "role-id"
 :discord/ticket-roles
 {"release-slug" "role-id"}}
```

## Mux livestreams

Mux live streams are created outside Compass with a signed playback policy.
Configure their playback IDs and Ti.to release access in environment-specific
configuration:

```clj
{:mux/streams
 [{:id "main-stage"
   :title "Main Stage"
   :playback-id "signed-playback-id"
   :allowed-ticket-slugs #{"streaming" "regular-conference"}}]
 :mux/signing-key-id "signing-key-id"
 :mux/signing-private-key "base64-encoded-private-key"
 :mux/playback-token-ttl-seconds 43200}
```

Keep `:mux/signing-private-key` in `config.local.edn`, an environment variable,
or system credentials. Never commit it.

### Creating a stream

`bin/create-mux-stream` creates a signed live stream through the Mux API and
adds its signed playback ID and ticket access rules to `:mux/streams` in
`config.local.edn`. It requires Babashka and a Mux API access token with Video
write permission.

Mux API access tokens consist of two values: a token ID and token secret. These
are separate from the URL-signing key used by Compass to generate playback
tokens, and from the stream key used by OBS. Supply the API credentials as
environment variables:

```sh
export MUX_TOKEN_ID="..."
export MUX_TOKEN_SECRET="..."
```

Alternatively, put them in the ignored `config.local.edn` file:

```clj
{:mux/token-id "..."
 :mux/token-secret "..."}
```

Run the script with a URL-safe Compass stream ID, display title, and one or
more allowed Ti.to release slugs:

```sh
bin/create-mux-stream main-stage "Main Stage" streaming regular-conference
```

All arguments are optional. Running `bin/create-mux-stream` without arguments
uses `main-stage`, `Main Stage`, and `streaming` respectively.

Mux live streaming, including test streams, requires a Pay As You Go or higher
plan. The Free plan supports on-demand video only. On an eligible paid plan,
pass `--test` before the other arguments to create a test stream that does not
incur live-stream usage charges:

```sh
bin/create-mux-stream --test
bin/create-mux-stream --test main-stage "Main Stage" streaming
```

Mux test streams display a watermark, stop after five active minutes, and
delete their recorded asset after 24 hours. Create another test stream when the
five-minute limit has been reached.

The script performs the following operations:

1. Creates a Mux live stream with a signed playback policy.
2. Configures its recorded asset to use signed playback as well.
3. Copies the current local configuration to `config.local.edn.bak`.
4. Adds the stream to `:mux/streams`, replacing any local entry with the same
   Compass stream ID.
5. Prints the Mux resource and playback IDs, followed by the OBS server, secret
   stream key, and combined ingest URL.

Use the printed values in OBS under **Settings → Stream → Custom**. The server
is `rtmps://global-live.mux.com:443/app`; enter the stream key separately in
the Stream Key field. Treat that key and the combined URL as secrets.

Each invocation creates a new remote Mux stream, even when the Compass stream
ID already exists locally. Replaced Mux streams are not deleted automatically.
The script exits without changing `config.local.edn` when authentication or
stream creation fails.

## Roadmap

See [[notes.txt]] for a basic outline of what we have planned. We will
(probably) not be able to do all of it this edition, but we should have the
basics of creating/editing activities, and signing up for them.

Currently the initial setup is then, we have Discord OAuth working, and have a
database connection, which gets pre-seeded with the talks from the schedule. The
front page renders all activities, is somewhat mobile-friendly, and does dark
and light mode. (important ;) 

## Dev setup

```
bin/launchpad dev --go
```

## License

Copyright &copy; 2024 Arne Brasseur and Contributors

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
