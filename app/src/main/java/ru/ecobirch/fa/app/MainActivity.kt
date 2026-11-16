package ru.ecobirch.fa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.ecobirch.fa.app.ui.navigation.EcoBirchRoot
import ru.ecobirch.fa.app.ui.state.MainViewModel
import ru.ecobirch.fa.app.ui.theme.BetulaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetulaTheme {
                val vm: MainViewModel = viewModel()
                EcoBirchRoot(viewModel = vm)
            }
        }
    }
}
