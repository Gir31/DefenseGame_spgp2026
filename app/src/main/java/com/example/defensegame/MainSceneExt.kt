package com.example.defensegame

import a2dg.scene.World
import a2dg.view.GameContext

// Enemy, WaveGen 등 게임 오브젝트가 World 에 접근할 때 쓰는 확장 함수이다.
fun GameContext.mainWorld(): World<MainScene.Layer> = (scene as MainScene).world

// 기지 HP, 처치 보상, 게임 종료 판정을 위한 접근자
fun GameContext.gameManager(): GameManager = (scene as MainScene).gameManager
