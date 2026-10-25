package app.podiumpodcasts.podium

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import app.podiumpodcasts.podium.ui.PlayerScreen
import app.podiumpodcasts.podium.ui.PlayerViewModel

@Composable
fun PodiumApp() {
    val viewModel: PlayerViewModel = viewModel()
    PlayerScreen(viewModel = viewModel)
}
