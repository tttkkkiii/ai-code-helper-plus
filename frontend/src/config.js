/**
 * 接口配置 —— 换环境时只改这里
 *
 * 默认走 Vite 代理（/api -> http://localhost:8081），可以规避浏览器跨域。
 * 如果要直连后端，在项目根目录建 .env.local 写：
 *   VITE_API_BASE_URL=http://localhost:8081/api
 * 同时记得让后端放行跨域（加 @CrossOrigin 或全局 CORS 配置）。
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * 聊天接口路径
 *
 * 对应后端：
 *   @RestController
 *   @RequestMapping("/ai")
 *   public class AiController {
 *     @GetMapping("/chat") ...
 *   }
 *
 * ⚠️ 如果你的后端没有 @RequestMapping("/ai")，请把这里改成 '/chat'
 */
export const CHAT_PATH = '/ai/chat'
