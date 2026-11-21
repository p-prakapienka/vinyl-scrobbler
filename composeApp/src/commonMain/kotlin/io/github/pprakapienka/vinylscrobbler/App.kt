package io.github.pprakapienka.vinylscrobbler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Vinyl Scrobbler",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = "Recognition provider: AcoustID",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Milestone 1: shared recognition and scrobbling core",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
