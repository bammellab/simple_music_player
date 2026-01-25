package com.bammellab.musicplayer.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bammellab.musicplayer.BuildConfig
import com.bammellab.musicplayer.R

private const val INSTALLATION_URL = "https://github.com/bammellab/simple_music_player/blob/master/INSTALLATION.md"
private const val SOURCE_CODE_URL = "https://github.com/bammellab/simple_music_player"

@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    onShowDescription: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bammellaboneline),
                    contentDescription = "Bammellab Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                AboutMenuItem(
                    title = stringResource(R.string.about_this_app),
                    subtitle = stringResource(R.string.about_this_app_subtitle),
                    onClick = onShowDescription
                )

                HorizontalDivider(color = Color(0xFF505050))

                AboutMenuItem(
                    title = stringResource(R.string.installation_title),
                    subtitle = stringResource(R.string.installation_subtitle),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(INSTALLATION_URL))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = Color(0xFF505050))

                AboutMenuItem(
                    title = stringResource(R.string.source_code_title),
                    subtitle = stringResource(R.string.source_code_subtitle),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_CODE_URL))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = Color(0xFF505050))

                AboutMenuItem(
                    title = stringResource(R.string.version_label),
                    subtitle = BuildConfig.VERSION_NAME,
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
fun AboutDescriptionDialog(
    onDismiss: () -> Unit,
    onBack: () -> Unit
) {
    Dialog(onDismissRequest = onBack) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bammellaboneline),
                    contentDescription = "Bammellab Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                Text(
                    text = stringResource(R.string.about_description_text01),
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun AboutMenuItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp
        )
        Text(
            text = subtitle,
            color = Color(0xFFCCCCCC),
            fontSize = 12.sp
        )
    }
}
