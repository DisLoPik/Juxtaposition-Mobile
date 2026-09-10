import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // SafeAreaRegions.all covers both the container and the keyboard. Compose applies
            // its own safe-area and IME insets, so letting SwiftUI inset as well would double
            // the padding under text fields.
            .ignoresSafeArea(.all)
    }
}
