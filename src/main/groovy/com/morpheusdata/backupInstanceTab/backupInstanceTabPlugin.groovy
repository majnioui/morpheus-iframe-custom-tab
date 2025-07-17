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
            name: "Tab Name",
            code: "backup-instance-tab-name",
            fieldName: "instanceTabName",
            displayOrder: 0,
            fieldLabel: "Tab Name",
            helpText: 'Enter Tab name',
            required: true,
            inputType: OptionType.InputType.TEXT
        )

        this.settings << new OptionType(
            name: "Tab Title",
            code: "backup-instance-tab-title",
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