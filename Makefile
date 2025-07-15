.PHONY: build

build:
	## make is now a task, which builds plugin, moves it, then removes build artifacts
	./gradlew make

tag:
	## create a tag version for the template which the plugin template
	## has been tested to to work with
	@echo "Enter Morpheus version tested compatible with (format morpheusx.x.x).."; \
	read MORPHEUS_TESTED; \
	git tag -a $$MORPHEUS_TESTED -m "Creating Tag reference (compatible with morpheusx.x.x) "$$MORPHEUS_TESTED; \
	git push origin $$MORPHEUS_TESTED