(ns lambdaisland.compass.html.profiles
  "Views and components (hiccup/ornament) related to profiles"
  {:ornament/prefix "profiles-"}
  (:require
   [lambdaisland.compass.css.tokens :as t :refer :all]
   [lambdaisland.compass.db.queries :as queries]
   [lambdaisland.compass.html.components :as c]
   [lambdaisland.compass.http.routing :refer [url-for]]
   [lambdaisland.compass.model.user :as user]
   [markdown-to-hiccup.core :as m]
   [lambdaisland.ornament :as o]))

;; UI of profile detail

(o/defstyled edit-profile-btn :a.btn
  ([user]
   [:<>
    {:href (url-for :profile/edit)} "Edit Profile"]))

(o/defstyled profile-detail :div#detail
  [c/image-frame :w-100px {t/--arc-thickness "7%"}]
  [:.details
   [:.bio :mt-2]
   [:.links :my-2]
   [:.link :w-full :flex :flex-1 :py-1 :font-size-3]
   [:.link-type :w-12 :px-2]
   [:.link-ref :flex-grow :px-2]]
  [:.contact-card :my-6 :shadow-3
   {:background-color t/--surface-2
    :padding t/--size-3
    :border-radius t/--size-2}]
  ([{:public-profile/keys [name hidden?]
     :user/keys [uuid] :as user} viewer]
   [:<>
    [:div [c/image-frame {:profile/image (user/avatar-css-value user)}]]
    [:div.details
     [:h3.title name]
     (when (:public-profile/bio user)
       [:div.bio
        (m/component (m/md->hiccup (:public-profile/bio user)))])
     [:div.links
      (for [link (:public-profile/links user)]
        [:div.link
         [:div.link-type (:profile-link/type link)]
         [:div.link-ref (:profile-link/href link)]])]]
    ;; if the user is connected (contact), show "contact card"
    (when (some #{(:db/id viewer)} (map :db/id (:user/contacts user)))
      [:div.contact-card
       [:div.details
        [:h3.title (:private-profile/name user)]
        (when (:private-profile/bio user)
          [:div.bio
           (m/component (m/md->hiccup (:private-profile/bio user)))])
        [:div.links
         (for [link (:private-profile/links user)]
           [:div.link
            [:div.link-type (:profile-link/type link)]
            [:div.link-ref (:profile-link/href link)]])]]])
    ;; hide edit button unless it's your own profile
    (when (= (:db/id viewer) (:db/id user))
      [:div.actions
       [edit-profile-btn user]])]))

(o/defstyled row :tr.link-row
  ([{:keys [variant] :as link}]
   [:<>
    [:td
     (when (:db/id link)
       [:input {:type "hidden" :name (str variant "-link-id[]") :value (:db/id link)}])
     (let [link-type (:profile-link/type link)]
       [:select {:name (str variant "-link-type[]")}
        [:option {:value "email" :selected (= link-type "email")} "Email"]
        [:option {:value "mastodon" :selected (= link-type "mastodon")} "Mastodon"]
        [:option {:value "linkedin" :selected (= link-type "linkedin")} "LinkedIn"]
        [:option {:value "personal-site" :selected (= link-type "personal-site")} "Personal Site"]
        [:option {:value "other" :selected (= link-type "other")} "Other"]])]
    [:td
     [:input
      {:type "text"
       :name (str variant "-link-ref[]")
       :value (str (:profile-link/href link))}]]]))

(def always-show ["email" "mastodon"])

(o/defstyled links-table :div
  :flex-col :gap-1
  [:.add-link {:align-self "center"}]
  ([links {:keys [caption variant]}]
   (let [link-map (into {} (map (juxt :profile-link/type :profile-link/href)) links)
         link-vals (concat
                    (for [t always-show]
                      [t (get link-map t)])
                    (apply dissoc link-map always-show))]
     [:<>
      [:template
       [row {:profile-link/type "other"
             :profile-link/href ""
             :variant variant}]]
      [:table
       #_[:thead
          [:tr
           [:th {:colspan 2} caption]]]
       [:tbody
        (for [[t h] link-vals]
          [row {:profile-link/type t
                :profile-link/href h
                :variant variant}])]]
      [:input.add-link
       {:value "+ Add Link"
        :type "button"
        :on-click "let form = this.parentElement; form.querySelector('tbody').append(form.querySelector('template').content.cloneNode(true))"}]])))

(o/defstyled profile-form :div#form.form-card-styling
  [c/image-frame :w-100px {t/--arc-thickness "7%"}]
  [:table :w-full]
  [:.contact-card
   :flex-col :gap-3
   :shadow-3
   :my-6
   {:background-color t/--surface-3
    :padding t/--size-3
    :border-radius t/--size-2}]
  ([user]
   [:<>
    [:h2 "Edit Profile"]
    [:form {:method "POST" :action "/profile/save" :enctype "multipart/form-data"}
     [:input {:type "hidden" :name "user-id" :value (:db/id user)}]
     [:label {:for "name"}
      [:span "Display Name"]
      [:input {:id "name" :name "name_public" :type "text"
               :required true
               :value (:public-profile/name user)}]]
     [:label {:for "bio_public"}
      [:span "Bio (accepts markdown)"]
      [:textarea {:id "bio_public" :name "bio_public"}
       (when (:public-profile/bio user)
         (:public-profile/bio user))]]
     [:label {:for "image"}
      [:span "Avatar"]
      (when user
        [c/image-frame {:profile/image (user/avatar-css-value user)}])
      [:input {:id "image" :name "image" :type "file" :accept "image/png, image/jpeg"}]]
     [:label.checkbox {:for "hiding"}
      [:span "Incognito mode"]
      [:span
       [:input {:id "hiding" :name "hidden?" :type "checkbox"
                :checked (:public-profile/hidden? user)}]
       [:span "Don't show my name and avatar on sessions I participate in."]]]
     [:label
      [:span "Public Profile Links"]
      [links-table (:public-profile/links user)
       {:variant "public"
        #_#_:caption "Public Profile Links"}]]

     [:div.contact-card
      [:h3 "Contact Card"]
      [:p.info "This information is only shown to people you add as a contact."]
      [:label {:for "name"}
       [:span "Name"]
       [:input {:id "name" :name "name_private" :type "text"
                :value (:private-profile/name user)}]]
      [:label {:for "bio_private"}
       [:span "Private Bio (accepts markdown)"]
       [:textarea {:id "bio_private" :name "bio_private"}
        (when (:private-profile/bio user)
          (:private-profile/bio user))]]
      [links-table (:private-profile/links user)
       {:variant "private"
        :caption "Links Visible to Contacts"}]]

     [:input {:type "submit" :value "Save Profile"}]]
    [:script
     "[...document.getElementsByClassName('add-link')].map((e)=>e.addEventListener('htmx:configRequest', function(evt) {
      const url = new URL(evt.detail.path, window.location.origin);
      var elements = document.querySelectorAll('tr.link-row');
      url.searchParams.set('row-index', elements.length);
      // update hidden field
      document.getElementById('rows-count').setAttribute('value', elements.length+1);
      // update URL
      evt.detail.path = url.toString();
     }));"]]))
