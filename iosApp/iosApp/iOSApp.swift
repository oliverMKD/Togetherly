import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        #if DEBUG
        TogetherlyIosInitializerKt.initializeTogetherlyIos(debug: true)
        #else
        TogetherlyIosInitializerKt.initializeTogetherlyIos(debug: false)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
