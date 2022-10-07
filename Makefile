default:
	mvn package

.ONESHELL:
test: default
	@gen_config="test/test-generator-config.json"
	@benchmark_config="test/test-config.json"
	@data_dir="test/test-data"
	@result_prefix="test_results"
	@
	@mkdir -p $${data_dir}/lib
	@zip -FSj $${data_dir}/lib/flexiblejoin.jar.zip target/*.jar
	@echo -n "generating config... "
	@python3 test/generate_test_config.py --config $${gen_config} --output $${benchmark_config} --data-root $${data_dir}
	@echo "done"
	@echo "retrieving query results..."
	@python3 test/asterixdb-benchmarking/benchmark.py --config $${benchmark_config} --data-root $${data_dir} --output $${result_prefix} --discard-runtimes --keep-results
	@echo "done"
	@echo "comparing query results..."
	@python3 test/compare_results.py --config $${gen_config} --data-root $${data_dir} --result-filename-prefix $${result_prefix}
	@echo "done"