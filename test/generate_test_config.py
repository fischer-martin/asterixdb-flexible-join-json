#!/usr/bin/env python3

import json
import argparse
import sys
import os
import shutil

# https://stackoverflow.com/a/14981125
def print_err(*args, **kwargs):
    print(*args, file = sys.stderr, flush = True, **kwargs)

def get_directory():
    return os.path.dirname(os.path.realpath(__file__))

def get_data_directory():
    return os.path.realpath(CONSOLE_ARGUMENTS.data_root)

def get_query_directory(run):
    return get_data_directory() + "/statements/" + run

def get_run_name_for_test(test, join_type):
    return test + "_" + join_type

def get_raw_queries_directory():
    return get_directory() + "/raw-queries"

def read_file_content(filename, is_json = False):
    with open(filename, "r") as file:
        if is_json:
            file_content = json.load(file)
        else:
            file_content = file.read()

    return file_content

def create_nop_query_file(filename, test):
    query_dir = get_query_directory(test)
    if not os.path.exists(query_dir):
        os.makedirs(query_dir)

    with open(query_dir + "/" + filename, "w") as file:
        file.write("-- nop\n;")


def set_up_preparation_files(test, datasets, join_types, generate_ids):
    def set_up_preparation_file(test):
        def format_file_and_format(preparation_query, dataset_num):
            formatted = preparation_query.replace("{{file{num}}}".format(num = dataset_num), datasets[str(dataset_num)]["file"])
            formatted = formatted.replace("{{format{num}}}".format(num = dataset_num), datasets[str(dataset_num)]["format"])
            return formatted
        
        filename = get_raw_queries_directory() + "/common/1.preparation." + ("join" if datasets["2"] else "self-join") + (".autogenerated_ids" if generate_ids else "") + ".sqlpp"
    
        preparation_query = read_file_content(filename)
        preparation_query = format_file_and_format(preparation_query, 1)
        if datasets["2"]:
            preparation_query = format_file_and_format(preparation_query, 2)

        query_dir = get_query_directory(test)
        if not os.path.exists(query_dir):
            os.makedirs(query_dir)
    
        with open(query_dir + "/1.preparation.sqlpp", "w") as preparation_query_file:
            preparation_query_file.write(preparation_query)

    def create_nop_preparation_query_file(test):
        create_nop_query_file("1.preparation.sqlpp", test)

    # ugly solution to reuse the same dataset or else we would have problems with the autogenerated UUIDs as primary keys (they would always differ)
    set_up_preparation_file(get_run_name_for_test(test, "init"))
    for join_type in join_types: 
        create_nop_preparation_query_file(get_run_name_for_test(test, join_type))
    create_nop_preparation_query_file(get_run_name_for_test(test, "cleanup"))

def set_up_benchmark_files(test, datasets, join_types):
    def set_up_benchmark_file(join_type):
        def format_subdoc_and_dataset(benchmark_query, num, subdoc, dataset_identifier):
            formatted = benchmark_query.replace("{{subdoc{num}}}".format(num = num), subdoc if subdoc else "")
            formatted = formatted.replace("{{dataset{num}}}".format(num = num), dataset_identifier)

            return formatted

        filename = get_raw_queries_directory() + "/" + join_type + "/2.benchmark.sqlpp"
        benchmark_query = read_file_content(filename)
        benchmark_query = format_subdoc_and_dataset(benchmark_query, 1, datasets["1"]["subdoc"], "DS1")
        if datasets["2"]:
            benchmark_query = format_subdoc_and_dataset(benchmark_query, 2, datasets["2"]["subdoc"], "DS2")
        else:
            benchmark_query = format_subdoc_and_dataset(benchmark_query, 2, datasets["1"]["subdoc"], "DS1")

        query_dir = get_query_directory(get_run_name_for_test(test, join_type))
        with open(query_dir + "/2.benchmark.sqlpp", "w") as benchmark_query_file:
            benchmark_query_file.write(benchmark_query)

    def create_nop_benchmark_query_file(test):
        create_nop_query_file("2.benchmark.sqlpp", test)

    create_nop_benchmark_query_file(get_run_name_for_test(test, "init"))
    for join_type in join_types:
        set_up_benchmark_file(join_type)
    create_nop_benchmark_query_file(get_run_name_for_test(test, "cleanup"))

def set_up_cleanup_files(test, join_types):
    def create_nop_cleanup_file(test):
        create_nop_query_file("3.cleanup.sqlpp", test)

    create_nop_cleanup_file(get_run_name_for_test(test, "init"))
    for join_type in join_types:
        create_nop_cleanup_file(get_run_name_for_test(test, join_type))

    filename = "3.cleanup.sqlpp"
    src = get_raw_queries_directory() + "/common/" + filename
    dst = get_query_directory(get_run_name_for_test(test, "cleanup")) + "/" + filename
    shutil.copyfile(src, dst)

def set_up_fj_creation_files(test, fj_join_types):
    for join_type in fj_join_types:
        filename = "4.fj-creation.sqlpp"
        src = get_raw_queries_directory() + "/" + join_type + "/" + filename
        dst = get_query_directory(get_run_name_for_test(test, join_type)) + "/" + filename
        shutil.copyfile(src, dst)

def set_up_warmup_files(test, datasets, join_types):
    def create_nop_warmup_file(test):
        create_nop_query_file("5.warmup.sqlpp", test)

    create_nop_warmup_file(get_run_name_for_test(test, "init"))
    for join_type in join_types:
        # we don't need no warmup since we are only interested in the query results and not in accurate execution times
        #set_up_warmup_file(join_type)
        create_nop_warmup_file(get_run_name_for_test(test, join_type))
    create_nop_warmup_file(get_run_name_for_test(test, "cleanup"))

def add_runs_to_output_config(output_config, test, join_types, fj_join_types, thresholds, join_library, query_timeouts = None):
    def add_run_to_output_config(join_type, thresholds):
        runs_config = output_config["runs"]
        run_name = get_run_name_for_test(test, join_type)
        runs_config.update({
                run_name: {
                    "enabled": True,
                    "config": {
                        "dataverse": "FJJEDITest",
                        "thresholds": thresholds
                    }
                }
            })
        if join_type in fj_join_types:
            runs_config[run_name]["config"].update({"join_library": join_library})
        if query_timeouts:
            runs_config[run_name]["config"].update({"query_timeouts": query_timeouts})

    add_run_to_output_config("init", [])
    for join_type in join_types:
        add_run_to_output_config(join_type, thresholds)
    add_run_to_output_config("cleanup", [])

argparser = argparse.ArgumentParser()
argparser.add_argument("-c", "--config", help = "config for generation of a benchmarking config", type = str, required = True)
argparser.add_argument("-o", "--output", help = "benchmarking config output", type = str, required = True)
argparser.add_argument("-r", "--data-root", help = "data directory", type = str, required = True)
argparser.add_argument("-sn", "--skip-native", help = "skips generation of runs for native JEDI implementation", action = "store_true")
argparser.add_argument("-snlf", "--skip-native-length-filter", help = "skips generation of runs for native JEDI + length filter implementation", action = "store_true")
argparser.add_argument("-snli", "--skip-native-label-intersection", help = "skips generation of runs for native JEDI + label intersection filter implementation", action = "store_true")
argparser.add_argument("-snjof", "--skip-native-jofilter", help = "skips generation of runs for native JEDI + JOFilter implementation", action = "store_true")
argparser.add_argument("-sfjlf", "--skip-fj-length-filter", help = "skips generation of runs for length filter FJ", action = "store_true")
argparser.add_argument("-sfjlfjo", "--skip-fj-length-filter-jofilter", help = "skips generation of runs for length filter with JOFilter FJ", action = "store_true")
argparser.add_argument("-sfjli", "--skip-fj-label-intersection", help = "skips generation of runs for label intersection FJ", action = "store_true")
argparser.add_argument("-sfjlijo", "--skip-fj-label-intersection-jofilter", help = "skips generation of runs for label intersection with JOFilter FJ", action = "store_true")
argparser.add_argument("-sfjsli", "--skip-fj-shadow-label-intersection", help = "skips generation of runs for label intersection shadow FJ", action = "store_true")
CONSOLE_ARGUMENTS = argparser.parse_args()

join_types = []
if not CONSOLE_ARGUMENTS.skip_native:
    join_types.append("native")
if not CONSOLE_ARGUMENTS.skip_native_length_filter:
    join_types.append("native_length_filter")
if not CONSOLE_ARGUMENTS.skip_native_label_intersection:
    join_types.append("native_intersection_lower_bound")
if not CONSOLE_ARGUMENTS.skip_native_jofilter:
    join_types.append("native_jofilter")
if not CONSOLE_ARGUMENTS.skip_fj_length_filter:
    join_types.append("fj_length_filter")
if not CONSOLE_ARGUMENTS.skip_fj_length_filter_jofilter:
    join_types.append("fj_length_filter_jofilter")
if not CONSOLE_ARGUMENTS.skip_fj_label_intersection:
    join_types.append("fj_label_intersection")
if not CONSOLE_ARGUMENTS.skip_fj_label_intersection_jofilter:
    join_types.append("fj_label_intersection_jofilter")
if not CONSOLE_ARGUMENTS.skip_fj_shadow_label_intersection:
    join_types.append("fj_shadow_label_intersection")
if not join_types:
    argparser.error("can't skip every join type")
fj_join_types = join_types.copy()
if "native" in fj_join_types:
    fj_join_types.remove("native")
if "native_length_filter" in fj_join_types:
    fj_join_types.remove("native_length_filter")
if "native_intersection_lower_bound" in fj_join_types:
    fj_join_types.remove("native_intersection_lower_bound")
if "native_jofilter" in fj_join_types:
    fj_join_types.remove("native_jofilter")


config = read_file_content(CONSOLE_ARGUMENTS.config, True)

output_config = {}
output_config.update({"connection_config": config["connection_config"]})
output_config.update({"query_timeouts": config["query_timeouts"]})
output_config.update({"runs": {}})

tests = config["tests"]

for test, test_config in tests.items():
    if test_config["enabled"]:
        datasets = test_config["datasets"]
        set_up_preparation_files(test, datasets, join_types, test_config["generate_ids"])
        set_up_benchmark_files(test, datasets, join_types)
        set_up_cleanup_files(test, join_types)
        set_up_fj_creation_files(test, fj_join_types)
        set_up_warmup_files(test, datasets, join_types)
        add_runs_to_output_config(output_config, test, join_types, fj_join_types, test_config["config"]["thresholds"], config["join_library"], test_config["config"]["query_timeouts"] if "query_timeouts" in test_config["config"].keys() else None)

with open(CONSOLE_ARGUMENTS.output, "w") as output_config_file:
    json.dump(output_config, output_config_file, indent = 4, ensure_ascii = False)
