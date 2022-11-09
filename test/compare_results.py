#!/usr/bin/env python3

import argparse
import filecmp
import json
import os
import sys

def get_data_directory():
    return os.path.realpath(CONSOLE_ARGUMENTS.data_root)

def get_results_directory():
    return get_data_directory() + "/results"

def get_run_name_for_test(test, join_type):
    return test + "_" + join_type

def read_file_content(filename, is_json = False):
    with open(filename, "r") as file:
        if is_json:
            file_content = json.load(file)
        else:
            file_content = file.read()

    return file_content

argparser = argparse.ArgumentParser()
argparser.add_argument("-c", "--config", help = "config file for generator tool", type = str, required = True)
argparser.add_argument("-r", "--data-root", help = "root of data directory", type = str, required = True)
argparser.add_argument("-f", "--result-filename-prefix", help = "prefix for output that has been passed to benchmarking tool", type = str, required = True)
argparser.add_argument("-snlf", "--skip-native-length-filter", help = "skips generation of runs for native JEDI + length filter implementation", action = "store_true")
argparser.add_argument("-snli", "--skip-native-label-intersection", help = "skips generation of runs for native JEDI + label intersection filter implementation", action = "store_true")
argparser.add_argument("-sfjlf", "--skip-fj-length-filter", help = "skips generation of runs for length filter FJ", action = "store_true")
argparser.add_argument("-sfjli", "--skip-fj-label-intersection", help = "skips generation of runs for label intersection FJ", action = "store_true")
argparser.add_argument("-sfjsli", "--skip-fj-shadow-label-intersection", help = "skips generation of runs for label intersection shadow FJ", action = "store_true")
CONSOLE_ARGUMENTS = argparser.parse_args()

join_types = ["native"]
if not CONSOLE_ARGUMENTS.skip_native_length_filter:
    join_types.append("native_length_filter")
if not CONSOLE_ARGUMENTS.skip_native_label_intersection:
    join_types.append("native_intersection_lower_bound")
if not CONSOLE_ARGUMENTS.skip_fj_length_filter:
    join_types.append("fj_length_filter")
if not CONSOLE_ARGUMENTS.skip_fj_label_intersection:
    join_types.append("fj_label_intersection")
if not CONSOLE_ARGUMENTS.skip_fj_shadow_label_intersection:
    join_types.append("fj_shadow_label_intersection")
optimized_join_types = join_types.copy()
optimized_join_types.remove("native")

config = read_file_content(CONSOLE_ARGUMENTS.config, True)

tests = config["tests"]

longest_join_type_string_len = len(max(join_types, key = len))
failed_tests = 0
for test, test_config in tests.items():
    if test_config["enabled"]:
        print("checking results of test {test}:".format(test = test))
        native_result_filename = get_results_directory() + "/" + get_run_name_for_test(test, join_types[0]) + "/" + CONSOLE_ARGUMENTS.result_filename_prefix + ".json"
        for optimized_test in optimized_join_types:
            optimized_result_filename = get_results_directory() + "/" + get_run_name_for_test(test, optimized_test) + "/" + CONSOLE_ARGUMENTS.result_filename_prefix + ".json"

            if not os.path.isfile(optimized_result_filename):
                print("  {optimized_test}:{pad} FAILED (result file not found)".format(optimized_test = optimized_test, pad = " " * (longest_join_type_string_len - len(optimized_test))))
                failed_tests += 1
                continue

            if filecmp.cmp(native_result_filename, optimized_result_filename):
                print("  {optimized_test}:{pad} PASSED".format(optimized_test = optimized_test, pad = " " * (longest_join_type_string_len - len(optimized_test))))
            else:
                print("  {optimized_test}:{pad} FAILED".format(optimized_test = optimized_test, pad = " " * (longest_join_type_string_len - len(optimized_test))))
                failed_tests += 1
                native_res = read_file_content(native_result_filename, True)
                optimized_res = read_file_content(optimized_result_filename, True)
                for thresh in native_res.keys():
                    print("    threshold {thresh}: {status}".format(thresh = thresh, status = "PASSED" if native_res[thresh] == optimized_res[thresh] else "FAILED"))

print("number of failed tests: {failed_tests}".format(failed_tests = failed_tests))
if failed_tests > 0:
    sys.exit(1)
