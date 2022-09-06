#!/usr/bin/bash

if [ "$#" -ne 1 ]; then
    echo "usage: $0 result_file_prefix"
    exit 1
fi

exit_code=0

RESULTS_PREFIX="$1"
RESULTS_DIR="results/$RESULTS_PREFIX"

source query_data.sh

for thresh in ${THRESHOLDS[@]}; do
    diff -q "$RESULTS_DIR/${JEDI_PREFIX}__${RESULTS_PREFIX}__t$thresh.txt" "$RESULTS_DIR/${JEDI_LEN_FILTER_PREFIX}__${RESULTS_PREFIX}__t$thresh.txt" || exit_code=1
    diff -q "$RESULTS_DIR/${JEDI_PREFIX}__${RESULTS_PREFIX}__t$thresh.txt" "$RESULTS_DIR/${FJ_INTERVAL_PREFIX}__${RESULTS_PREFIX}__t$thresh.txt" || exit_code=1
    diff -q "$RESULTS_DIR/${JEDI_PREFIX}__${RESULTS_PREFIX}__t$thresh.txt" "$RESULTS_DIR/${FJ_LABEL_INTERSECTION_PREFIX}__${RESULTS_PREFIX}__t$thresh.txt" || exit_code=1
done

exit $exit_code
