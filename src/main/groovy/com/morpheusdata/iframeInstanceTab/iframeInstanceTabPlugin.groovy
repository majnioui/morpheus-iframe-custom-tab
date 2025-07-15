package com.morpheusdata.iframeInstanceTab

import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Permission
import com.morpheusdata.views.HandlebarsRenderer
import com.morpheusdata.model.OptionType
import groovy.util.logging.Slf4j

@Slf4j
class IframeInstanceTabPlugin extends Plugin {

    String code = "iframe-instance-tab"

	@Override
	void initialize() {
	    // set additional metadata
		this.setName("BackupPowerM")
		this.setDescription("Custom tab Integration by PowerM")
		this.setAuthor("PowerM")

        // call and register the tab provider
        IframeInstanceTabProvider iframeInstanceTabProvider = new IframeInstanceTabProvider(this, morpheus)
        this.pluginProviders.put(iframeInstanceTabProvider.code, iframeInstanceTabProvider)

        //this.setRenderer(new HandlebarsRenderer(this.classLoader))

        // create a permission
		this.setPermissions([Permission.build('PowerM Instance Tab Plugin','iframe-instance-tab', [Permission.AccessType.none, Permission.AccessType.full])])

	    // configuration options for iframe

	    this.settings << new OptionType(
            name: "Tab Name",
            code: "iit-instance-tab-name",
            fieldName: "instanceTabName",
            displayOrder: 0,
            fieldLabel: "Tab Name",
            helpText: 'Enter Tab name',
            required: true,
            inputType: OptionType.InputType.TEXT
        )

        this.settings << new OptionType(
            name: "Tab Title",
            code: "iit-tab-title",
            fieldName: "instanceTabTitle",
            displayOrder: 1,
            fieldLabel: "Tab Title",
            helpText: 'Title to appear at the top of the Instance Tab view',
            required: true,
            inputType: OptionType.InputType.TEXT
        )

	}

	@Override
	void onDestroy() {}
}