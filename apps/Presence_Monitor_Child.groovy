definition(
    name: "Presence Monitor Child",
    namespace: "dcm.presence",
    author: "Dominick Meglio",
    description: "Provides options for combined presence devices",
    category: "My Apps",
	parent: "dcm.presence:Presence Monitor",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "prefProxyDevice")
	page(name: "prefSettings")
}

def prefProxyDevice() {
	return dynamicPage(name: "prefProxyDevice", title: "Create a Child Proxy Device", nextPage: "prefSettings", uninstall:false, install: false) {
		section("General") {
            label title: "Enter a name for this child app. This will create a virtual presence sensor which reports the presence based on the settings in this app.", required:true
        }
	}
}

def prefSettings() {
	createOrUpdateChildDevice()
    return dynamicPage(name: "prefSettings", title: "", install: true, uninstall: true) {
		section("Always Accurate For Present") {
			paragraph "The device chosen for this setting is always accurate when it says you are present but may not be accurate when it says not present (for example Wifi Phone Sensors)"
			input "alwaysPresent", "capability.presenceSensor", title: "Presence Sensors for Accurate Present", multiple:true, required:false
		}
		section("Accurate When Combined For Present") {
			paragraph "When the specified number of devices chosen for this setting say present, assume you are present"
			input "combinedPresent", "capability.presenceSensor", title: "Presence Sensors for Combined Present", multiple:true, required:false
            input "combinedPresentCount", "number", title: "How many must show present?", required: false, defaultValue: 1
		}
		section("Combined Accurate For Not Present") {
			paragraph "When the specified number of devices chosen for this setting say not present, assume you are not present"
			input "combinedNotPresent", "capability.presenceSensor", title: "Presence Sensors for Combined Not Present", multiple:true, required:false
            input "combinedNotPresentCount", "number", title: "How many must show not present?", required: false, defaultValue: 1
			input "combinedPresenceDelay", "number", title: "Only report a departure if the device(s) stay not present for the specified number of minutes", required: false, defaultValue: 0
		}
        section("General") {
            input("debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false)
        }
	}
}

def installed() {
	initialize()
}

def uninstalled() {
	logDebug "uninstalling app"
	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}
}

def updated() {	
    logDebug "Updated with settings: ${settings}"
	unschedule()
    unsubscribe()
	initialize()
}

def initialize() {
	if (alwaysPresent != null)
		subscribe(alwaysPresent, "presence.present", trustArrivalHandler)
	if (combinedPresent != null)
		subscribe(combinedPresent, "presence.present", combinedArrivalHandler)
	if (combinedNotPresent != null)
		subscribe(combinedNotPresent, "presence.not present", combinedDepartureHandler)
}

def trustArrivalHandler(evt) {
	def device = getChildDevice(state.presenceDevice)
	state.departTime = -1
	device.arrived()
	logDebug "trusted arrival detected"
}

def combinedArrivalHandler(evt) {
	def device = getChildDevice(state.presenceDevice)
	logDebug "Arrival detected"
	def totalPresent = 0
	
	combinedPresent.each { it ->
		if (it.currentValue("presence") == "present")
		{
			logDebug "${it.name} -> ${it.currentValue("presence")}"
			totalPresent++
		}
	}
    if (totalPresent >= combinedPresentCount) 
    {
		state.departTime = -1
        logDebug "Threshold hit, setting proxy to present"
		device.arrived()
    }
	else
		logDebug "Arrival occurred but threshold was not reached ${totalPresent} < ${combinedPresentCount}"
}

def combinedDepartureHandler(evt) {
	def device = getChildDevice(state.presenceDevice)
	logDebug "Departure detected"
	
	def totalNotPresent = 0
	combinedNotPresent.each { it ->
		if (it.currentValue("presence") == "not present")
		{
			logDebug "${it.name} -> ${it.currentValue("presence")}"
			totalNotPresent++
		}
	}
	if (totalNotPresent >= combinedNotPresentCount)
    {
		if (combinedPresenceDelay > 0)
		{
			state.departTime = now()
			logDebug "Threshold hit, setting departure delay"
			runIn(combinedPresenceDelay*60, departureAfterDelay)
		}
		else
		{
			logDebug "Threshold hit, setting proxy to not present"
			device.departed()
		}
    }
	else
		logDebug "Departure occurred but threshold was not reached ${totalNotPresent} < ${combinedNotPresentCount}"
}

def departureAfterDelay() {
	if (state.departTime != -1 && now() - state.departTime > (combinedPresenceDelay*60)-10000)
	{
		def device = getChildDevice(state.presenceDevice)
		logDebug "Departure delay timer hit, setting proxy to not present"
		device.departed()
	}
	else
		logDebug "Departure delay hit but no longer departed, cancelled"
}

def createOrUpdateChildDevice() {
	def childDevice = getChildDevice("presence:" + app.getId())
    if (!childDevice || state.presenceDevice == null) {
        logDebug "Creating proxy device"
		state.presenceDevice = "presence:" + app.getId()
		addChildDevice("hubitat", "Virtual Presence", "presence:" + app.getId(), 1234, [name: app.label, isComponent: false])
    }
	else if (childDevice && childDevice.name != app.label)
		childDevice.name = app.label
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}