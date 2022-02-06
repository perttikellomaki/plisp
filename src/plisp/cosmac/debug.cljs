(ns plisp.cosmac.debug)

;;;
;;; Dumping of instructions and processor state.
;;;

(defn format-instruction
  "Format a single instruction."
  [instruction]
  (str
   (:op instruction)
   (when (:n instruction) (format " %x" (:n instruction)))
   (when (:immediate instruction) (format " #%02x" (:immediate instruction)))
   (when (:long-immediate instruction) (format " #%04x" (:long-immediate instruction)))
   (when (:page-address instruction) (format " %02x" (:page-address instruction)))
   (when (:long-address instruction) (format " %04x" (:long-address instruction)))
   (when (:value instruction) (format " %02x %s" (:value instruction) (char (:value instruction))))))

(defn dump-instruction
  "Dump a single instruction."
  [processor]
  (let [pc (get-in processor [:R (:P processor)])
        instruction
        (get-in processor
                [:mem pc])]
  (println (format "%04x: " pc)
           (format-instruction instruction))))

(defn dump-processor
  "Dump processor state."
  ([processor] (dump-processor processor processor))
  ([previous processor]
   (print (format "D: %02x  DF:%x  P: %x  X: %x\n"
                  (:D processor)
                  (:DF processor)
                  (:P processor)
                  (:X processor)))
   (dotimes [n 4]
     (print (format "R%x: %04x  R%x: %04x  R%x: %04x  R%x: %04x\n"
                    n (get-in processor [:R n])
                    (+ 4 n) (get-in processor [:R (+ 4 n)])
                    (+ 8 n) (get-in processor [:R (+ 8 n)])
                    (+ 12 n) (get-in processor [:R (+ 12 n)]))))
   (let [[_ changes _]  (diff previous processor)]
     (when (:mem changes)
       (doseq [[addr val] (:mem changes)]
         (print (format "mem %04x: %02x\n" addr (:value val))))))))

(defn format-address
  "Format memory address and its contents."
  [addr val]
  (str (format "%04x: " addr) (format-instruction val)))

(defn lst
  "List memory contents."
  ([start end execution]
   (let [memory (:mem (first execution))]
     (map (fn [addr] (format-address addr (get-in memory [addr])))
          (range start (+ end 1))))))

;;;
;;; Debug utilities.
;;;

(defn debug-lisp [input]
  (execution (prog) (reader (str input "\r\004"))))

(defn break-at [addr execution]
  (drop-while (fn [processor]
                (not= (get-in processor [:R (:P processor)])
                      addr))
              execution))

(defn trace [count execution]
  (doseq [processor (take count execution)]
    (dump-instruction processor)
    (dump-processor processor (next-state processor))))
