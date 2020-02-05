package com.gxd.alipay

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.alipay.sdk.app.PayTask
import com.gxd.alipay.util.OrderInfoUtil2_0

class MainActivity : AppCompatActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    fun payV2(v: View)
    {
        if (TextUtils.isEmpty(APPID) || TextUtils.isEmpty(RSA2_PRIVATE) && TextUtils.isEmpty(RSA_PRIVATE))
        {
            showAlert(this, getString(R.string.error_missing_appid_rsa_private))
            return
        }

        /*
		 * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
		 * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
		 * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
		 *
		 * orderInfo 的获取必须来自服务端；
		 */
        /*
		 * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
		 * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
		 * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
		 *
		 * orderInfo 的获取必须来自服务端；
		 */
        val rsa2: Boolean = RSA2_PRIVATE.isNotEmpty()
        val params: Map<String, String> = OrderInfoUtil2_0.buildOrderParamMap(APPID, rsa2)
        val orderParam: String = OrderInfoUtil2_0.buildOrderParam(params)

        val privateKey: String = if (rsa2) RSA2_PRIVATE else RSA_PRIVATE
        val sign: String = OrderInfoUtil2_0.getSign(params, privateKey, rsa2)
        val orderInfo = "$orderParam&$sign"

        val payRunnable = Runnable {
            val alipay = PayTask(this@MainActivity)
            val result = alipay.payV2(orderInfo, true)
            Log.i("msp", result.toString())
            val msg = Message()
            msg.what = SDK_PAY_FLAG
            msg.obj = result
            mHandler.sendMessage(msg)
        }

        // 必须异步调用
        // 必须异步调用
        val payThread = Thread(payRunnable)
        payThread.start()

    }

    private fun showAlert(ctx: Context, info: String)
    {
        showAlert(ctx, info, null)
    }

    private fun showAlert(ctx: Context, info: String, onDismiss: DialogInterface.OnDismissListener?)
    {
        AlertDialog.Builder(ctx)
                .setMessage(info)
                .setPositiveButton(R.string.confirm, null)
                .setOnDismissListener(onDismiss)
                .show()
    }

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler()
    {
        override fun handleMessage(msg: Message)
        {
            super.handleMessage(msg)
            when (msg?.what)
            {
                // 支付app调起回应
                SDK_PAY_FLAG  ->
                {
                    val payResult = PayResult(msg.obj as Map<String?, String?>)
                    /**
                     * 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                     */
                    /**
                     * 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                     */
                    val resultInfo: String = payResult.result // 同步返回需要验证的信息

                    val resultStatus: String = payResult.resultStatus
                    // 判断resultStatus 为9000则代表支付成功
                    // 判断resultStatus 为9000则代表支付成功
                    if (TextUtils.equals(resultStatus, "9000"))
                    { // 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                        showAlert(this@MainActivity, getString(R.string.pay_success) + payResult)
                    } else
                    { // 该笔订单真实的支付结果，需要依赖服务端的异步通知。
                        showAlert(this@MainActivity, getString(R.string.pay_failed) + payResult)
                    }
                }

                SDK_AUTH_FLAG ->
                {
                }
            }
        }
    }


    companion object
    {
        const val SDK_PAY_FLAG = 1

        const val SDK_AUTH_FLAG = 2

        /**
         * 用于支付宝支付业务的入参 app_id。
         */
        const val APPID = "2021001115652278"

        /**
         * 用于支付宝账户登录授权业务的入参 pid。
         */
        const val PID = ""

        /**
         * 用于支付宝账户登录授权业务的入参 target_id。
         */
        const val TARGET_ID = ""
        /**
         * pkcs8 格式的商户私钥。
         *
         * 如下私钥，RSA2_PRIVATE 或者 RSA_PRIVATE 只需要填入一个，如果两个都设置了，本 Demo 将优先
         * 使用 RSA2_PRIVATE。RSA2_PRIVATE 可以保证商户交易在更加安全的环境下进行，建议商户使用
         * RSA2_PRIVATE。
         *
         * 建议使用支付宝提供的公私钥生成工具生成和获取 RSA2_PRIVATE。
         * 工具地址：https://doc.open.alipay.com/docs/doc.htm?treeId=291&articleId=106097&docType=1
         */
        const val RSA2_PRIVATE = "MIIEpAIBAAKCAQEAh+n0MTPwYaqtjQpHDS9I4eNaznXnBsPgnoFUqLTeCjRu95utiQ/HyrONmFk7RZkWwTOqk/JtniHwYRrYeh5/d+IhGdq0oFA44rNhmdRbkidA3UWAqkKFLKIEfjiT1mlrjuzFlDUq9pnca0TkUVMNzV5xvHT+rBXvNOa73HruLFP+6dV+Fa1wdU3fkqD2R2u8T7vvBUbEnyFcuQ+7tHCKipA9pITF4yD148XvlesPZzOyCjQgxi22emhSvhXByTpSG78/mtR0/Ep0TeBy+ELLcJtOGeALHnywMsRdSWWYba29tktlP9H+P7SvZ2QoD+6tCmaXAA6WWRWBA8UeO4A7HQIDAQABAoIBAEIWXZnLMpibAHazR6c+lcbY4V/A7sxYVJFeO8co40q9AjQ+K5yY4sJKmpA421xVOz5InCoCWJDq2dKt1hNTXxHxD71dnjTRrUd2h//fxYMEuUKeuBiR7eWtBlhFtIUuj5FYyh3t2G0+lXJHdmmg4/Y3dUB49xjANadYhoKH9+XvYC+OOJLMQx0fv6Y8GxktyYDswWXZzTxS9fDATnhBAau/yaHaCUIC1oEOAPaY47jNajOelFAv7R+oMU/2KF4yhvABC1pRELDjL6mVzr9CPAypLcK20Xfu2xHRFsXfE+3S1yC36F446KPZ84Br8sesitg0/aLu9fE8A+OKnNggQHUCgYEAxbfBj8T0m4vPoB9fPbZRakmHE7M2t1bSQ7FA2GkMh1NfwkXIIF7XLkUe7jypNXpD9N+oNzkwpPFjJhBCVpBYHrPQereEyOqOriDqu4Ripgt/430r59c8eqRhNc1GKbVsZVH30cCMgFDzUd1ojIG5+fIZZDK50EU1n/lFw4sWoSsCgYEAr/pUtgyf0QE3hAPsiEBwwyDudSGICbLUg6C60ZWbecY4Xh2IVQ3PnrLR851hrU+BS74U26ekM13/tkoq+umedlHhlVwIm9WMX2oS9qins3VPAOTCLb9xm0Lz2M5gur+frHkPc8veGnL2zwYQHYGx/gAQ7ueMHeOYHaYA+EpMoNcCgYEAtEJSIKdqc53GD7BKftMH6yVaF2XezsxDKyAhhKxRldc3iMFiMdEF0dOG3aFEi9MKvehjTuW/KcJBqORN9unmzScXPaqvTxY6OxsbpxaNceEuGdN3AgpDw3S2g/zAqSoUx1Mx43PvJNWo4MxBk0h6rBxDdgyhR/axa0YyipL2cL0CgYEAq9ObGTLUbahpg46tSxDYyb6C932pLgceN/Qd1fzllY61hTgC4UGeAxAJrnxyXl4uGj3ccemLi9hxkZVzyBGKd29V+5eklDx8VsHlCFD/hlu5q16yNyciD37dSEmi61Wl3CtMqj1afarZUKVT4Ou9VwzLe+o8xwZuhiyL7+PkVHsCgYBymItNgtfHwf5nlQQyJ9Vp9y9afHONBBf0U6YALhQl8LP7UALaSONV/2ZBNi3fvNugiDSgbfJDiV+N2OMHikDPMf/pN8MAYKo6AZZNy1yRjWoOo4/dCbamRXhAqiXHwpkt6xb3PAJT4165qrkxgi/wyEJX/fxgbETFdPbX8xAiEg=="

        const val RSA_PRIVATE = ""
    }
}
