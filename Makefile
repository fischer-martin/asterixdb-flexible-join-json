default:
	mvn package

.ONESHELL:
test: default
	@gen_config="test/test-generator-config.json"
	@benchmark_config="test/test-config.json"
	@data_dir="test/test-data"
	@result_prefix="test_results"

	@rm -rf $${data_dir}/results
	@mkdir -p $${data_dir}/lib
	@zip -FSj $${data_dir}/lib/flexiblejoin.jar.zip target/*.jar
	@echo
	@echo
	@echo
	@echo -n "generating config... "
	@python3 test/generate_test_config.py --skip-fj-shadow-label-intersection --config $${gen_config} --output $${benchmark_config} --data-root $${data_dir}
	@echo "done"
	@echo
	@echo "retrieving query results..."
	@python3 test/asterixdb-benchmarking/benchmark.py --config $${benchmark_config} --data-root $${data_dir} --output $${result_prefix} --discard-runtimes --keep-results
	@echo "done"
	@echo
	@echo "comparing query results..."
	@python3 test/compare_results.py --skip-fj-shadow-label-intersection --config $${gen_config} --data-root $${data_dir} --result-filename-prefix $${result_prefix}
	@echo "done"

clean:
	rm -rf target
	rm -rf test/test-data/results
	rm -rf test/test-data/statements
	rm -rf test/test-data/lib
	rm -rf test/test-config.json
