package remix.myplayer.ui.nav

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 从 Activity 的 intent 到 Compose 导航的一次性请求通道。
 *
 * 为什么不用 NavDeepLink：本项目的 LocalNavController 是 composition 内
 * `rememberNavController()` 提供的，Activity 的 handleIntent() 拿不到它；
 * 用 StateFlow 暂存请求再由 AppNav 消费，可避免依赖 onResume 与首帧的先后顺序。
 */
object PendingRoute {

  val route = MutableStateFlow<String?>(null)

  fun request(route: String) {
    this.route.value = route
  }

  fun consume() {
    route.value = null
  }
}
