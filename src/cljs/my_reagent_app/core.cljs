(ns my-reagent-app.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react])
    (:import goog.History))

;; For debugging
(enable-console-print!)

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to my-reagent-app"]
   [:div [:a {:href "#/todos"} "go to todo list"]]])

;; Todo things

(def todos (atom [{:title "Make app"
                   :done false}
                  {:title "Make it work well"
                   :done false}]))

(defn find-thing [haystack needle]
  "Returns the index of of needle in a seq haystack"
  (first (keep-indexed #(when (= %2 needle) %1) haystack)))

(defn mark-as-done! [todo]
  "Marks todo with :id id as done and saves it to the global state"
  (swap! todos update-in [(find-thing @todos todo) :done] not))

(defn todo-item [todo]
  [:li
   {:key (find-thing @todos todo)
    :on-click (fn [x]
                (mark-as-done! todo))}
   (str (find-thing @todos todo) ": " (:title todo) (if (:done todo)
                                                      "☑"
                                                      "☐"))])

(defn add-todo! [title]
  "Creates a new todo item and adds it to the global state"
  {:pre [string? title]}
  (do
    (let [new-todo {:title title
                    :done false}]
      (swap! todos conj new-todo))))

(defn atom-input [value]
  [:form
   [:input {:type "text"
            :value @value
            :placeholder "Add new todo item"
            :on-change #(reset! value (-> % .-target .-value))}]
   [:button {:on-click #(do (add-todo! @value)
                            (reset! value "")
                            nil)}
    "Add todo"]])

(defn add-todo-field []
  (let [val (atom "")]
    (fn []
      [:div
       [:span "" [atom-input val] ]
       [:span @val]])))

(defn todos-page []
  [:div
   [:h2 "Att göra"]
   ;[:p (atom-input (atom "atom"))]
   [:p [add-todo-field]]
   [:hr]
   [:p (str "You have " (count @todos) " things to do")]
   [:div (doall (map todo-item @todos))]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/todos" []
  (session/put! :current-page #'todos-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
