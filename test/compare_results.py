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
CONSOLE_ARGUMENTS = argparser.parse_args()

join_types = ["native", "fj_interval", "fj_label_intersection"]
fj_join_types = join_types.copy()
fj_join_types.remove("native")

config = read_file_content(CONSOLE_ARGUMENTS.config, True)

tests = config["tests"]

failed_tests = 0
for test, test_config in tests.items():
    if test_config["enabled"]:
        print("checking results of test {test}:".format(test = test))
        native_result_filename = get_results_directory() + "/" + get_run_name_for_test(test, join_types[0]) + "/" + CONSOLE_ARGUMENTS.result_filename_prefix + ".json"
        for fj_test in fj_join_types:
            fj_result_filename = get_results_directory() + "/" + get_run_name_for_test(test, fj_test) + "/" + CONSOLE_ARGUMENTS.result_filename_prefix + ".json"
            if filecmp.cmp(native_result_filename, fj_result_filename):
                print("  {fj_test}: PASSED".format(fj_test = fj_test))
            else:
                print("  {fj_test}: FAILED".format(fj_test = fj_test))
                failed_tests += 1
                native_res = read_file_content(native_result_filename, True)
                fj_res = read_file_content(fj_result_filename, True)
                for thresh in native_res.keys():
                    print("    threshold {thresh}: {status}".format(thresh = thresh, status = "PASSED" if native_res[thresh] == fj_res[thresh] else "FAILED"))

print("number of failed tests: {failed_tests}".format(failed_tests = failed_tests))
if failed_tests > 0:
    sys.exit(1)
