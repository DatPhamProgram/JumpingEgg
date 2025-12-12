package com.datpv.myapplication.view.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.datpv.myapplication.view.instruction.InstructionScreen
import com.datpv.myapplication.view.theme.JumpingEggTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JumpingEggTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}

@Composable
fun AppNavigation (navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = "home"
    ){
        composable ("home") {
            HomeScreen(
                onStartGameClick = {},
                onRankingClick = {},
                onInstructionClick = {navController.navigate("instruction")},
                onDoaGameClick = {}

            )
        }

        // Instruction screen
        composable ("instruction") {
            InstructionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("game") {
            SimpleStubScreen("Game Screen") { navController.popBackStack() }
        }
        composable("ranking") {
            SimpleStubScreen("Ranking Screen") { navController.popBackStack() }
        }
        composable("doa") {
            SimpleStubScreen("D.O.A Game Screen") { navController.popBackStack() }
        }
    }
}

@Composable
private fun SimpleStubScreen(
    title: String,
    onBack: () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onBack,
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        androidx.compose.material3.Text(text = "$title - Back")
    }
}
