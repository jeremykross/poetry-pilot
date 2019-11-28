(ns poetry-pilot.entity
  (:require
    [entr.components :as component]
    [entr.entity :as entity]
    [ulmus.signal :as ulmus]))

(def keydown-$ (ulmus/from-event (.-body js/document) "keydown"))
(def keyup-$ (ulmus/from-event (.-body js/document) "keyup"))

(defn key-down-signal
  [code])

(defn key-up-signal
  [code])

(def input-mapping {37 :left
                    38 :up
                    39 :right
                    40 :down})

(def direction-mapping
  {:left [-1 0 0]
   :up [0 1 0]
   :right [1 0 0]
   :down [0 -1 0]})

(def movement-direction-$
  (ulmus/merge
    (ulmus/map
      #(get input-mapping (.-keyCode %) :static)
      keydown-$)
    (ulmus/map
      #(if (= (get input-mapping (.-keyCode %)) @movement-direction-$)
         :static
         @movement-direction-$)
      keyup-$)))

(def movement-vector-$
  (ulmus/distinct
    (ulmus/map
      (fn [dir]
        (get direction-mapping dir [0 0 0]))
      movement-direction-$)))

(defn move-toward
  [t-$ target-$ rate start]
  (ulmus/reduce
    (fn [v target]
      (let [diff (- target v)
            dir (js/Math.sign diff)
            amt-to-change (* (min (js/Math.abs diff)
                                  (js/Math.abs rate)) dir)]
        (+ v amt-to-change)))
    start
    (ulmus/sample-on t-$ target-$)))

(defn player
  [t-$]
  (let [speed 5
        rotation-z-target-$ (ulmus/map
                              (fn [dir]
                                (condp = dir
                                  :up (/ js/Math.PI -8)
                                  :down (/ js/Math.PI 8)
                                  0))
                              movement-direction-$)
        rotation-$ (ulmus/map
                     (fn [[_ z]]
                       [0
                        (/ js/Math.PI 2)
                        z])
                     (ulmus/zip
                       t-$
                       (move-toward t-$ rotation-z-target-$ 0.03 0)))
        position-$
        (ulmus/reduce
          (fn [pos direction]
            (when direction
              (map + pos (map #(* % speed) direction))))
          [0 0 0]
          (ulmus/sample-on t-$ movement-vector-$))]
    (entity/entity
      {:k :poetry-pilot/fire 
       :fire-$ (ulmus/filter (fn [e]
                               (and e
                                    (= (.-keyCode e)
                                       32)))
                             keydown-$)}
      (component/transform position-$ rotation-$ (ulmus/constant [30 30 30]))
      (component/statically component/obj-mesh "meshes/D1.obj" "textures/D1.png"))))
    
      
(defn cube
  [t-$ pos]
  (entity/entity
    (component/transform
      (ulmus/reduce
        (fn [pos _]
          (map - pos [1.5 0 0]))
        pos
        t-$)
      (ulmus/reduce
        (fn [rot _]
          (map + rot [0 0.1 0]))
        [0 0 0]
        t-$)
      (ulmus/constant [1 1 1]))
    (component/statically component/box-mesh 35 35 35)))

(defn shot
  [t-$ origin-$]
  (entity/entity
    (component/transform
      (ulmus/reduce
        (fn [pos _]
          (map + pos [16 0 0]))
        (or @origin-$ [0 0 0])
        t-$)
      (ulmus/reduce
        (fn [rot _]
          (map + rot [0.2 0 0]))
        [0 0 0]
        t-$)
      (ulmus/constant [1 1 1]))
    (component/statically component/color 0x1111ff)
    (component/statically component/box-mesh 72 4 4)))

