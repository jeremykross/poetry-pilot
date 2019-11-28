(ns poetry-pilot.core
  (:require
    [entr.components :as component]
    [entr.entity :as entity]
    [entr.renderer :as renderer]
    [entr.scene :as scene]
    entr.renderers.three
    [poetry-pilot.entity :as pp-entity]
    [poetry-pilot.util :as util]
    [ulmus.signal :as ulmus]
    [ulmus.transaction :as ulmus-tr]))

(defn clear!
  [ctx w h]
  (set! (.-fillStyle ctx) "black")
  (.fillRect ctx 0 0 w h))

(defn offset
  [width height slice t]
  (let [[s-height rate] slice
        x (mod (* rate t) width)]
    [[x height (- width x) s-height, 0 height (- width x) s-height]
     [0 height (- width (- x width)) s-height, (- width x) height (- width (- x width)) s-height]]))

(defn render-background!
  [ctx bg-img slices t]
  (loop [remaining-slices slices
         height 0]
    (when-let [slice (first remaining-slices)]
      (doseq [[sx sy sw sh dx dy dw dh] (offset 640 height slice t)]
        (.drawImage ctx bg-img sx sy sw sh dx dy dw dh))
      (recur (rest remaining-slices) (+ height (first slice))))))

(defn create-scene!
  [w h tick-$]
  (let [player (pp-entity/player tick-$)
        entities-$
        (ulmus/reduce
          (fn [entities fire]
            (conj
              entities
              (pp-entity/shot tick-$
                              (get-in player
                                      [:components
                                       :entr/transform
                                       :position-$]))))
          [(entity/entity
             (component/statically
               component/transform [0 0 100] [0 0 0] [1 1 1])
             (component/statically
               component/ortho-camera
               (/ w -2) (/ w 2) (/ h 2) (/ h -2)  1.0 1000.0))
           player
           (pp-entity/cube tick-$ [320 100 10])
           (pp-entity/cube tick-$ [700 -100 10])
           (pp-entity/cube tick-$ [1200 0 0])]
          (get-in player [:components :poetry-pilot/fire :fire-$]))
        scene (scene/Scene.
                nil
                (entr.renderers.three/three-renderer! w h {:alpha true
                                                           :canvas (.getElementById js/document "foreground")})
                entities-$)]
    (scene/init! scene)
    scene))

(defn setup!
  []

  (let [w 640
        h 360
        slices [[64 1] [32 0.4] [16 0.3] [32 0.1], [32 0.2] [32 0.25] [32 0.3], [8 0.4] [16 0.6] [32 0.8] [64 1]]
        color-cast [[0     [255 215 32 0.5]]
                    [5000  [255 255 255 0]]
                    [20000 [255 255 255 0]]
                    [25000 [0 0 96 0.5]]]

        bg (.getElementById js/document "beach")
        canvas (.getElementById js/document "background")
        context (.getContext canvas "2d")
        tick-$ (ulmus/input)
        scene (create-scene! w h tick-$)
        start-time (js/Date.now)
        start! (fn render! []
                 (ulmus-tr/>! tick-$ 1)
                 ; overwriting?
                 (ulmus-tr/propogate!)
                 (clear! context w h)
                 (render-background! context bg slices (/ (js/Date.now) 4))

                 (let [[r g b bg-opacity]
                       (or
                         (util/lerp-keyframes color-cast (- (js/Date.now) start-time))
                         [0 0 96 0.5])]
                   (.setClearColor @(:webgl-ref (:renderer scene))
                                   (js/THREE.Color. (/ r 255) (/ g 255) (/ b 255)) bg-opacity))

                 (scene/tick! scene)
                 (js/requestAnimationFrame render!))]

      (start!)))


(comment .addEventListener js/document "DOMContentLoaded" setup!)


