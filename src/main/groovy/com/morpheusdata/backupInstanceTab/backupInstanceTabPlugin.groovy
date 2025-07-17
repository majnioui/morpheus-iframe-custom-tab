package com.morpheusdata.backupInstanceTab

import com.morpheusdata.core.Plugin
import com.morpheusdata.model.Permission
import com.morpheusdata.views.HandlebarsRenderer
import com.morpheusdata.model.OptionType
import groovy.util.logging.Slf4j

@Slf4j
class BackupInstanceTabPlugin extends Plugin {

    String code = "backup-instance-tab"

	@Override
	void initialize() {
	    // set additional metadata
		this.setName("Backup PowerM")
		this.setDescription("Custom tab Integration by PowerM")
		this.setAuthor("PowerM")

        // call and register the tab provider
        BackupInstanceTabProvider backupInstanceTabProvider = new BackupInstanceTabProvider(this, morpheus)
        this.pluginProviders.put(backupInstanceTabProvider.code, backupInstanceTabProvider)

        //this.setRenderer(new HandlebarsRenderer(this.classLoader))

        // create a permission
		this.setPermissions([Permission.build('PowerM Instance Tab Plugin','backup-instance-tab', [Permission.AccessType.none, Permission.AccessType.full])])

	    // configuration options for the plugin

	    this.settings << new OptionType(
            name: "Instance Tab Name",
            code: "bit-instance-tab-name",
            fieldName: "instanceTabName",
            displayOrder: 0,
            fieldLabel: "Instance Tab Name",
            helpText: 'Enter a name for this Tab',
            required: true,
            inputType: OptionType.InputType.TEXT
        )

        this.settings << new OptionType(
            name: "Tab Title",
            code: "bit-tab-title",
            fieldName: "instanceTabTitle",
            displayOrder: 1,
            fieldLabel: "Instance Tab Title",
            helpText: 'Enter a title to appear at the top of the Instance Tab view',
            required: true,
            inputType: OptionType.InputType.TEXT
        )

        this.settings << new OptionType(
            name: "API Base URL",
            code: "backup-instance-tab-api-base-url",
            fieldName: "apiBaseUrl",
            displayOrder: 2,
            fieldLabel: "API Base URL",
            helpText: 'Enter the base URL for the API (e.g., https://portal.cdc.atlascs.ma)',
            required: true,
            inputType: OptionType.InputType.TEXT
        )

        this.settings << new OptionType(
            name: "Username",
            code: "backup-instance-tab-api-username",
            fieldName: "apiUsername",
            displayOrder: 3,
            fieldLabel: "Username",
            helpText: 'Enter the username for API authentication',
            required: true,
            inputType: OptionType.InputType.TEXT
        )

        this.settings << new OptionType(
            name: "Password",
            code: "backup-instance-tab-api-password",
            fieldName: "apiPassword",
            displayOrder: 4,
            fieldLabel: "Password",
            helpText: 'Enter the password for API authentication',
            required: true,
            inputType: OptionType.InputType.PASSWORD
        )

	}

	@Override
	void onDestroy() {}
}