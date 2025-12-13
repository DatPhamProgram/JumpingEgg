package com.datpv.myapplication.view
import com.datpv.myapplication.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp


@Composable
fun HomeScreen(
    onStartGameClick:() -> Unit,
    onRankingClick:() -> Unit,
    onInstructionClick:() -> Unit,
    onDoaGameClick: () ->Unit
) {

    Box(
        modifier =  Modifier.fillMaxSize()
    ){
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription =  null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MenuButton(
                resId = R.drawable.top_btn_01,
                contentDescription = "Start Game",
                onClick = onStartGameClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuButton(
                resId = R.drawable.top_btn_02,
                contentDescription = "Ranking",
                onClick = onRankingClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuButton(
                resId = R.drawable.top_btn_03,
                contentDescription = "Instruction",
                onClick = onInstructionClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuButton(
                resId = R.drawable.top_btn_04,
                contentDescription = "D.O.A Game",
                onClick = onDoaGameClick
            )
        }
    }
}

@Composable
private fun MenuButton(
    resId: Int,
    contentDescription: String,
    onClick:() -> Unit
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier =  Modifier.wrapContentWidth().clickable(onClick = onClick)
    )
}