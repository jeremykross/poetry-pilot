(ns poetry-pilot.util)

(defn lerp-keyframes
  [keyframes t]
  (let [frames-with-next (map-indexed (fn [i frame]
                                        [frame (get keyframes (inc i))]) keyframes)]
    (some (fn [[[t0 v0] [t1 v1]]]
            (if (and (>= t t0) (<= t t1))
              (let [pct (/ (- t t0) (- t1 t0))]
                (map + v0 (map #(* pct %) (map - v1 v0))))))
          frames-with-next)))
