(ns lambdaisland.compass.css.styles
  "Top level CSS rules"
  (:require
   [lambdaisland.compass.css.tokens :as t]
   [lambdaisland.ornament :as o]))

(o/defrules resets
  [[:p {:max-inline-size "inherit"}]
   [#{:ul :ol} :list-none :m-0 :p-0]
   [:body :overflow-x-hidden :w-screen]
   [#{:h1 :h2 :h3 :h4 :h5}
    {:color t/--text-1
     :max-inline-size "inherit"}]

   ;; override open-props normalize, we like the buttons a bit more rounded
   [#{:button :.btn} {:border-radius t/--radius-2}]

   ;; reset dialog
   [:dialog :p-0]

   [:body {:font-family "Open Sans, sans-serif"}]

   [:.site-copy
    [:p {:line-height 2}]
    [:h1 {:margin-top t/--size-5
          :margin-bottom t/--size-4}]
    [:h2 {:margin-top t/--size-4
          :margin-bottom t/--size-3}]
    [:h3 {:margin-top t/--size-3
          :margin-bottom t/--size-2}]
    [:h4 {:margin-top t/--size-2
          :margin-bottom t/--size-1}]
    [:h5 {:margin-top t/--size-1}]

    [:ul :py-2 [:li :py-1 :list-disc :list-inside]]
    [:ol :py-2 [:li :py-1 :list-decimal :list-inside]]]

   [:.form-card-styling
    :flex-col :gap-4
    {:background-color t/--surface-2
     :padding t/--size-3
     :border-radius t/--size-3}
    [:form :flex-col :gap-3]
    [:label
     :flex-col :gap-1
     {:font-size t/--font-size-3
      :font-weight t/--font-weight-6}
     #_["&:has([type=checkbox])"
        :flex
        :gap-3]]
    ["[type=submit]" {:align-self "end"
                      :background-color t/--highlight}]

    ;; [:label.checkbox [#_optional :span caption]
    ;;  [:span
    ;;   [:input {:type "checkbox"}]
    ;;   [:span "Explanation"]]]
    [:.checkbox [:>span:last-child
                 :flex-row :gap-2 :font-normal :items-center]]
    ]
   ])
