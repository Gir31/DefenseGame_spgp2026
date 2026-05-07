package a2dg.scene

import a2dg.view.GameContext

class SceneStack(private val gctx: GameContext) {
    private val scenes = mutableListOf<Scene>()

    val top: Scene?
        get() = scenes.lastOrNull()

    val size: Int
        get() = scenes.size

    val isEmpty: Boolean
        get() = scenes.isEmpty()

    fun push(scene: Scene) {
        top?.onPause()
        scenes.add(scene)
        gctx.scene = scene
        scene.onEnter()
    }

    fun pop(): Scene {
        val popped = scenes.removeAt(scenes.lastIndex)
        popped.onExit()
        top?.let {
            gctx.scene = it
            it.onResume()
        }
        return popped
    }

    // change 는 top 을 다른 Scene 으로 바꾸고, 나머지 stack 은 그대로 둔다.
    // push / pop 을 따로 호출하면 아래 Scene 이 잠깐 resume 되었다가 다시 pause 되므로,
    // change 는 그 중간 과정을 거치지 않고 맨 위 Scene 하나만 바로 교체한다.
    fun change(scene: Scene): Scene {
        if (scenes.isEmpty()) {
            push(scene)
            return scene
        }

        val previous = scenes.removeAt(scenes.lastIndex)
        previous.onExit()
        scenes.add(scene)
        gctx.scene = scene
        scene.onEnter()
        return previous
    }
}