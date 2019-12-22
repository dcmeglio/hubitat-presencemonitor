definition(
    name: "Presence Monitor",
    namespace: "dcm.presence",
    author: "Dominick Meglio",
    description: "Provides options for combined presence devices",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
     page(name: "mainPage", title: "", install: true, uninstall: true)
} 

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
	// Do nothing for now
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    	isInstalled()
		if(state.appInstalled == 'COMPLETE'){
			section("${app.label}") {
				paragraph "Provides options for combined presence devices"
			}
			section("Presence Child Apps") {
				app(name: "monitorApp", appName: "Presence Monitor Child", namespace: "dcm.presence", title: "Add a new Presence Monitor", multiple: true)
			}
			section("General") {
       			label title: "Enter a name for parent app (optional)", required: false
 			}
		}
	}
}

def isInstalled() {
	state.appInstalled = app.getInstallationState() 
	if (state.appInstalled != 'COMPLETE') {
		section
		{
			paragraph "Please click <b>Done</b> to install the parent app."
		}
  	}
}     
