(ns clj-dcss.char
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string  :as string]
            [clj-dcss.abbrev :as abbrev]))

(def ^:private char->tree
  (-> "char.ebnf" io/resource slurp insta/parser))

(defn- tree->map [x]
  (insta/transform
    {:text str
     :number #(Integer/parseInt %)
     :resist-val (fn [arg] (string/replace arg " " ""))
     :meta  (fn [& args] [:meta (into {} args)])
     :stats (fn [& args] [:stats (into {} args)])
     :resistances (fn [& args] [:resistances (into {} args)])
     :summary (fn [& args]
                (let [a (into {} args)
                      nt (.split (:name-and-title a) " the ")]
                  (-> (dissoc a :name-and-title)
                      (assoc :name (string/trim (first nt)))
                      (assoc :title (string/trim (last nt))))))
     :char  (fn [& args] (into {} args))}
    x))

(defn parse-char
  "Parse a DCSS character file (char dump, morgue file) into a Clojure map."
  [x] (-> x (string/split #"Inventory:") first char->tree tree->map))

(defn- count-piety-stars [points]
  (->> points
       (map str)
       (filter (partial = "*"))
       count))

(defn count-piety [x]
  (if (get-in x [:stats :piety])
    (update-in x [:stats :piety] count-piety-stars)
    x))

(defn move-godtitle [x]
  (if-let [gt (get-in x [:stats :godtitle])]
    (-> (assoc x :godtitle gt)
        (update-in [:stats] #(dissoc % :godtitle)))
    x))

(defn move-god [x]
  (if-let [g (get-in x [:stats :god])]
    (-> (assoc x :god g)
        (update-in [:stats] #(dissoc % :god)))
    x))

(defn parse-character-str [x]
  (if (string? (:character x))
    (update-in x [:character] abbrev/normalize-char)
    x))

(defn process-char
  "Process a DCSS character map, making it more useful and normalized."
  [x] (-> x count-piety move-godtitle move-god parse-character-str))

(defn parse-and-process-char
  "Parse and process a DCSS character file (char dump, morgue file)."
  [x] (-> x parse-char process-char))
