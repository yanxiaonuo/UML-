package com.xiaonuo.myweather;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.lljjcoder.Interface.OnCityItemClickListener;
import com.lljjcoder.bean.CityBean;
import com.lljjcoder.bean.DistrictBean;
import com.lljjcoder.bean.ProvinceBean;
import com.lljjcoder.citywheel.CityConfig;
import com.lljjcoder.style.citypickerview.CityPickerView;
import com.xiaonuo.myweather.dynamicweather.BaseDrawer;
import com.xiaonuo.myweather.dynamicweather.DynamicWeatherView;

import java.util.List;

import interfaces.heweather.com.interfacesmodule.bean.Lang;
import interfaces.heweather.com.interfacesmodule.bean.Unit;
import interfaces.heweather.com.interfacesmodule.bean.weather.now.Now;
import interfaces.heweather.com.interfacesmodule.view.HeConfig;
import interfaces.heweather.com.interfacesmodule.view.HeWeather;

public class MainActivity extends AppCompatActivity {


    /**
     * 获取城市
     */
    CityPickerView mPicker = new CityPickerView();


    /**
     * 标签
     */
    private String TAG = "HomeFragment";


    /**
     * 刷新控件
     */
    private SwipeRefreshLayout refreshLayout;


    /**
     * 动态天气控件
     */
    private DynamicWeatherView weatherView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivitiesCollection.addActivity(this);

        //ActionBar隐藏，透明化任务栏
        Utils.hideTaskbar(this);

        //初始化城市选择
        initPickCity();

        //初始化城市选择按钮
        initTouchPickCityButton();


        //和风天气初始化
        HeConfig.init("HE1905221751301283", "b41b189386254410a64e9676d990c9d7");

        //设定免费端口
        HeConfig.switchToFreeServerNode();

        //获取动态天气控件
        weatherView = findViewById(R.id.homeFragment_dynamicWeatherView_dynamicWeatherView);

        //更新温度，体感，天气描述 TextView的数据，数据来源（SP）
        updateWeatherTextView();

        //更新显示的地址（数据来源SP）
        updateRegionTextView();


        //动态背景的初始化
        initDynamicWeatherView();

        //刷新控件的初始化
        initSwipeRefreshLayout();

        //拉取天气数据
        updateWeatherSP();
    }

    /**
     * 更新温度，体感，天气描述 TextView的数据
     * 数据来源（SP）
     */
    public void updateWeatherTextView() {
        String json = Utils.getString(MainActivity.this, Constant.WEATHER_JSON_DATA, null);
        if (json != null) {
            final String[] dataFromWeatherJson = new WeatherFilter().getDataFromWeatherJson(json);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //温度
                    TextView temperature = findViewById(R.id.homeFragment_textView_temperature);
                    temperature.setText(dataFromWeatherJson[0]);

                    //cond_txt（天气描述），fl（体感温度）
                    TextView typeAndRealFeel = findViewById(R.id.homeFragment_textView_typeAndRealFeel);
                    typeAndRealFeel.setText(dataFromWeatherJson[1] + " | 体感 " + dataFromWeatherJson[2] + " °");

                    //刷新时间
                    TextView visibility = findViewById(R.id.homeFragment_textView_updateTime);
                    visibility.setText(dataFromWeatherJson[3] + " 数据");
                }
            });
        }
    }


    /**
     * 刷新控件的初始化
     * 设置颜色
     * 设置刷新内容
     * （1.获取和风天气的数据，Json存入SP）
     */
    private void initSwipeRefreshLayout() {
        //获取刷新控件
        refreshLayout = findViewById(R.id.main_swipeRefreshLayout_refreshLayout);
        //设定控件颜色（主题色）
        refreshLayout.setColorSchemeResources(R.color.theme);

        //设定刷新事件
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //更新sp天气数据
                updateWeatherSP();
            }
        });


    }

    /**
     * 获取和风天气数据
     * 存入sp
     * 更新sp天气数据
     * 更新温度，体感，天气描述 TextView的数据
     * 更新动态背景
     */
    public void updateWeatherSP() {

        //获取刷新控件
        refreshLayout = findViewById(R.id.main_swipeRefreshLayout_refreshLayout);

        //设定进入刷新状态
        refreshLayout.setRefreshing(true);

        //天气监听器
        HeWeather.OnResultWeatherNowBeanListener onResultWeatherNowBeanListener = new HeWeather.OnResultWeatherNowBeanListener() {
            /**
             * 获取失败
             * @param e :错误信息
             */
            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError: ", e);

                //开启UI更新线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //关闭刷新
                        refreshLayout.setRefreshing(false);

                        //吐司提示
                        Toast.makeText(MainActivity.this, "刷新失败", Toast.LENGTH_LONG).show();
                    }
                });
            }

            /**
             * 获取成功
             * @param dataObject :获取的天气数据集合
             */
            @Override
            public void onSuccess(List<Now> dataObject) {
                final String json = new Gson().toJson(dataObject);
                Log.d(TAG, "onSuccess: " + json);

                //子线程操作大任务
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //开启UI更新线程
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //关闭刷新
                                refreshLayout.setRefreshing(false);

                                //吐司提示
                                Toast.makeText(MainActivity.this, "刷新成功", Toast.LENGTH_LONG).show();
                            }
                        });

                        //存入天气json
                        Utils.putString(MainActivity.this, Constant.WEATHER_JSON_DATA, json);

                        //更新温度，体感，天气描述 TextView的数据
                        updateWeatherTextView();

                        //更新动态背景
                        updateDynamicWeatherView();

                    }
                }).start();

            }
        };

        //天气具体地址（区+城市）如雨湖,湘潭
        //获取SP中的地区，不存在则使用默认地区
        String district = Utils.getString(MainActivity.this, Constant.LOCAL_DISTRICT, Constant.DEFAULT_DISTRICT);

        //获取SP中的城市，不存在则使用默认城市
        String city = Utils.getString(MainActivity.this, Constant.LOCAL_CITY, Constant.DEFAULT_CITY);

        String specificAddress = district + "," + city;

        //获取天气
        //指定区域城市，语言，默认单位（米，英尺）
        HeWeather.getWeatherNow(MainActivity.this, specificAddress, Lang.CHINESE_SIMPLIFIED, Unit.METRIC, onResultWeatherNowBeanListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        new Thread(new Runnable() {
            @Override
            public void run() {
                weatherView.onPause();
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                weatherView.onResume();
            }
        }).start();
    }


    @Override
    public void onDestroy() {
        weatherView.onDestroy();
        super.onDestroy();
    }

    /**
     * 动态背景的初始化
     */
    private void initDynamicWeatherView() {
        // 更新动态背景 数据来源（SP）
        updateDynamicWeatherView();
    }

    /**
     * 更新动态背景
     * 数据来源（SP）
     */
    public void updateDynamicWeatherView() {

        //获取本地Json
        String json = Utils.getString(MainActivity.this, Constant.WEATHER_JSON_DATA, null);

        if (json != null) {
            //获取天气描述
            int x = json.indexOf("cond_txt");
            int y = json.indexOf(":", x);
            int j = json.indexOf(",", x);
            String temp = json.subSequence(y + 2, j - 1).toString();

            //进行天气的设定
            if (temp.contains("雨") && temp.contains("雪")) {
                weatherView.setDrawerType(BaseDrawer.Type.RAIN_SNOW_D);
            } else if (temp.contains("雨")) {
                weatherView.setDrawerType(BaseDrawer.Type.RAIN_D);
            } else if (temp.contains("雪")) {
                weatherView.setDrawerType(BaseDrawer.Type.SNOW_D);
            } else if (temp.contains("晴")) {
                weatherView.setDrawerType(BaseDrawer.Type.CLEAR_D);
            } else if (temp.contains("云")) {
                weatherView.setDrawerType(BaseDrawer.Type.CLOUDY_D);
            } else if (temp.contains("阴")) {
                weatherView.setDrawerType(BaseDrawer.Type.OVERCAST_D);
            } else if (temp.contains("雾")) {
                weatherView.setDrawerType(BaseDrawer.Type.FOG_D);
            } else if (temp.contains("霾")) {
                weatherView.setDrawerType(BaseDrawer.Type.HAZE_D);
            } else if (temp.contains("沙") || temp.contains("尘")) {
                weatherView.setDrawerType(BaseDrawer.Type.SAND_D);
            } else if (temp.contains("风")) {
                weatherView.setDrawerType(BaseDrawer.Type.WIND_D);
            } else {
                weatherView.setDrawerType(BaseDrawer.Type.UNKNOWN_D);
            }
        } else {
            weatherView.setDrawerType(BaseDrawer.Type.UNKNOWN_D);
        }

        //动态背景唤醒
        weatherView.onResume();
    }


    /**
     * 初始化城市选择按钮
     */
    private void initTouchPickCityButton() {

        TextView region = findViewById(R.id.homeFragment_textView_region);

        //设定点击监听
        region.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //显示
                mPicker.showCityPicker();
            }
        });
    }

    /**
     * 初始化城市选择
     */
    private void initPickCity() {

        //预先加载仿iOS滚轮实现的全部数据
        mPicker.init(MainActivity.this);

        //配置选择城市信息
        CityConfig cityConfig = new CityConfig.Builder()
                //标题
                .title("选择城市")

                //标题文字大小
                .titleTextSize(18)

                //标题文字颜  色
                .titleTextColor("#585858")

                //标题栏背景色
                .titleBackgroundColor("#E9E9E9")

                //确认按钮文字颜色
                .confirTextColor("#31c27c")

                //确认按钮文字
                .confirmText("确定")

                //确认按钮文字大小
                .confirmTextSize(16)

                //取消按钮文字颜色
                .cancelTextColor("#585858")

                //取消按钮文字
                .cancelText("取消")

                //取消按钮文字大小
                .cancelTextSize(16)

                //显示类，只显示省份一级，显示省市两级还是显示省市区三级
                .setCityWheelType(CityConfig.WheelType.PRO_CITY_DIS)

                //是否显示半透明背景
                .showBackground(true)

                //显示item的数量
                .visibleItemsCount(5)

                //默认显示的省份
                .province("湖南省")

                //默认显示省份下面的城市
                .city("湘潭市")

                //默认显示省市下面的区县数据
                .district("雨湖区")

                //省份滚轮是否可以循环滚动
                .provinceCyclic(true)

                //城市滚轮是否可以循环滚动
                .cityCyclic(true)

                //区县滚轮是否循环滚动
                .districtCyclic(true)

                //滚轮不显示模糊效果
                .drawShadows(true)

                //中间横线的颜色
                .setLineColor("#03a9f4")

                //中间横线的高度
                .setLineHeigh(5)

                //是否显示港澳台数据，默认不显示
                .setShowGAT(true)
                .build();

        //使设置生效
        mPicker.setConfig(cityConfig);

        //监听选择情况
        mPicker.setOnCityItemClickListener(new OnCityItemClickListener() {
            /**
             * 成功选择城市
             * @param province：省份
             * @param city：城市
             * @param district：地区
             */
            @Override
            public void onSelected(ProvinceBean province, CityBean city, DistrictBean district) {
                //存省份到SP（去除“省”用于和风天气查询）
                Utils.putString(MainActivity.this, Constant.LOCAL_PROVINCE, province.getName().replaceAll("省", ""));

                //存市到SP（去除“市”用于和风天气查询）
                Utils.putString(MainActivity.this, Constant.LOCAL_CITY, city.getName().replaceAll("市", ""));

                //存区到SP（去除“区”和“市”用于和风天气查询）
                Utils.putString(MainActivity.this, Constant.LOCAL_DISTRICT, district.getName().replaceAll("市", "")
                        .replaceAll("区", ""));

                //吐司提示
                Toast.makeText(MainActivity.this, "设定成功", Toast.LENGTH_SHORT).show();

                //更新本机的地址（数据来源SP）
                updateRegionTextView();

                //更新天气
                updateWeatherSP();
            }

            /**
             * 取消
             */
            @Override
            public void onCancel() {

            }
        });
    }


    /**
     * 更新显示的地址（数据来源SP）
     */
    private void updateRegionTextView() {
        TextView region = findViewById(R.id.homeFragment_textView_region);

        //获取SP中的地区，不存在则使用默认地区
        String district = Utils.getString(this, Constant.LOCAL_DISTRICT, Constant.DEFAULT_DISTRICT);

        //获取SP中的城市，不存在则使用默认城市
        String city = Utils.getString(this, Constant.LOCAL_CITY, Constant.DEFAULT_CITY);

        //设定文本
        region.setText(district + "-" + city);
    }

    public void gotoWeb(View view) {
        startActivity(new Intent(MainActivity.this, WebviewActivity.class));
    }


}
