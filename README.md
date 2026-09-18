# Compass

<strong><a href="#screenshots">Jump to Screenshots</a></strong>

Compass is a conference companion app, where attendees can find the schedule,
sign up for workshops or activities, access the live streams, and where they can
add contacts, to keep in touch with other attendees after the event.

Compass allows any attendee to create their own activities, and so is especially
well suited for community and unconference style events with a high level of
participation. People can star/bookmark or sign up for their favorite
activities/sessions. Activity organizers can limit the capacity, so e.g. if you
want to take up to 5 people to the climbing gym or go for Thai dinner with up to
8 people you can do that.

Compass integrates with Discord for auth, and Tito for ticketing. People log in
with their Discord account, which adds them to the conference Discord server,
and based on their ticket they get specific Discord roles (e.g. speaker,
organiser, sponsor). It's also possible to automatically create a thread with
everyone who signed up for a session, so you can discuss particulars there. We
may add other login options in the future, but these integration features do
make it quite attractive if your event does happen to use Discord.

Compass was originally developed for [Heart of
Clojure](https://heartofclojure.eu), and is being used for the
[Clojure/conj](https://clojure-conj.org) and [HeartConf](https://heartconf.eu)
conferences. It was inspired by the old [Eurucamp Activities
App](https://github.com/heartofclojure/activities). Eurucamp really pioneered
attendee-led activities during and in the fringe of the event. We hope Compass
may carry that torch into the future.

Compass is optimised for use on both desktop and mobile, and follows the
light/dark mode settings of the user's operating system, making it a pleasant
experience to use on the go during the event. The app can be fully styled and
customised to follow the house style of the event.

## Hosting Compass

Compass is a self-hosted Clojure application, using Datomic as its database.
It's not especially hard to host it yourself, but it's not trivial either. For a
hands-off hosted solution, including importing your schedule, and
branding/styling, contact Arne from [Magpie Solutions](https://magpie.software/).

While we'd love to offer a free hosted version for FOSS community events, that's
currently not feasible, because hosting does cost money, and because there's a
bit of manual setup needed for each event. That said for non-commercial events
that strengthen the FOSS ecosystem we're happy to provide a heavily discounted
rate.

## Technologies

- Clojure backend + HTMX
- Datomic
- Discord for Auth
- Integrant
- [Ornament](https://github.com/lambdaisland/ornament) and [Open Props](https://open-props.style/)

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

Livestreams are managed from Compass itself, at `/admin/livestreams`
(requires a crew ticket, see [Admin access](#admin-access) below). Creating a
stream there creates a signed live stream through the Mux API and stores its
id, playback ID, and allowed Ti.to release slugs in the database — there is no
`:mux/streams` config to hand-edit.

Compass still needs a few Mux-related config values, since these are
credentials rather than data managed through the UI:

```clj
{:mux/token-id "..."
 :mux/token-secret "..."
 :mux/signing-key-id "signing-key-id"
 :mux/signing-private-key "base64-encoded-private-key"
 :mux/playback-token-ttl-seconds 43200}
```

- `:mux/token-id` / `:mux/token-secret` — a Mux API access token with Video
  write permission, used by the admin UI to create live streams.
- `:mux/signing-key-id` / `:mux/signing-private-key` — the URL-signing key
  used by Compass to generate playback tokens for viewers. Separate from the
  API token above, and from the stream key used by OBS.

Keep all of these in `config.local.edn`, environment variables, or system
credentials. Never commit them.

### Creating a stream

Open `/admin/livestreams` and fill in the form: a URL-safe Compass stream ID
(e.g. `main-stage`), a display title, and the Ti.to release slugs that should
have access. Optionally check "Create as Mux test stream" — Mux live
streaming, including test streams, requires a Pay As You Go or higher plan;
the Free plan supports on-demand video only. Test streams display a
watermark, stop after five active minutes, and delete their recorded asset
after 24 hours.

Submitting the form creates a Mux live stream with a signed playback policy
(and configures its recorded asset the same way), then shows the OBS server,
secret stream key, and combined ingest URL once. Use those values in OBS
under **Settings → Stream → Custom**: the server is
`rtmps://global-live.mux.com:443/app`, with the stream key entered separately
in the Stream Key field. Treat that key and the combined URL as secrets — Mux
does not show the stream key again after creation.

Deleting a stream from `/admin/livestreams` removes it from Compass but does
not delete the underlying Mux resource; do that from the Mux dashboard if
needed.

### Admin access

`/admin/*` routes require the signed-in user to have a Ti.to ticket whose
release slug is `crew`.

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

## Screenshots

<img src="screenshots/2024-09-20_125803.png">
<img src="screenshots/2024-09-20_125913.png">
<img width="300" src="screenshots/Screen Shot 2026-09-18 at 13.16.05.png">
<img width="300" src="screenshots/Screen Shot 2026-09-18 at 13.16.08.png">
<img src="screenshots/Screen Shot 2026-09-18 at 13.16.27.png">
<img src="screenshots/Screen Shot 2026-09-18 at 13.16.30.png">
<img src="screenshots/Screen Shot 2026-09-18 at 13.17.23.png">

## License

Copyright &copy; 2024-2026 Arne Brasseur and Contributors

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
