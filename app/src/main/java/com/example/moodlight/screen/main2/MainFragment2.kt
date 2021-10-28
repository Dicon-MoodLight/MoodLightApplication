package com.example.moodlight.screen.main2

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.moodlight.R
import com.example.moodlight.api.ServerClient
import com.example.moodlight.databinding.FragmentMain2Binding
import com.example.moodlight.model.myanswermodel.MyAnswerListModel
import com.example.moodlight.model.myanswermodel.MyAnswerListModelItem
import com.example.moodlight.screen.main2.calendar.CalendarHelper
import com.example.moodlight.screen.main2.calendar.Main2CalendarAdapter
import com.example.moodlight.screen.main2.calendar.Main2CalendarData
import com.example.moodlight.screen.main2.calendar.Main2CalendarViewModel
import com.example.moodlight.screen.main2.diaryRecyclerview.data.DateAdapter
import com.example.moodlight.util.DataType
import kotlin.collections.ArrayList
import com.example.moodlight.screen.main2.diaryRecyclerview.data.DateClass
import com.example.moodlight.screen.main2.diaryRecyclerview.data.QnAData
import com.example.moodlight.util.GetTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat

class MainFragment2 : Fragment() {

    private val calendarHelper by lazy { CalendarHelper() }
    private var writePostMap: Map<String, *>? = null

    private val viewModel: Main2ViewModel by lazy {
        ViewModelProvider(this).get(Main2ViewModel::class.java)
    }

    private val calendarViewModel: Main2CalendarViewModel by lazy {
        ViewModelProvider(this).get(Main2CalendarViewModel::class.java)
    }

    private lateinit var binding: FragmentMain2Binding
    var list: ArrayList<DateClass> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main2, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.fragment = this
        Log.e("version", "3")

//        CoroutineScope(Dispatchers.IO).launch {
//            FirebaseUtil.getFireStoreInstance().collection("users")
//                .document(FirebaseUtil.getUid())
//                .get()
//                .addOnCompleteListener {
//                    writePostMap = it.result!!.get("writePostMap") as Map<String, *>?
//                    setUi()
//                }
//        }


        if(list.isEmpty()){
            dataLoding()
        }



        return binding.root
    }

    private fun dataLoding() {
        CoroutineScope(Dispatchers.IO).launch { 
            ServerClient.getApiService().getMyAnswer(0, 10)
                .enqueue(object : Callback<MyAnswerListModel> {
                    override fun onResponse(
                        call: Call<MyAnswerListModel>,
                        response: Response<MyAnswerListModel>
                    ) {
                        val result = response.body()
                        Log.d(TAG, "onResponse: $response")
                        if(response.code() == 200){
                            Log.d(TAG, "onResponse: data : $result")
                            processingData(result)
                            requireActivity().runOnUiThread {
                                binding.recycler.adapter = DateAdapter(requireContext(), list){data ->
                                    Toast.makeText(requireActivity(), "${data.id}", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(requireActivity(), DetailAnswerActivity::class.java)
                                        .putExtra("data", data))
                                }
                                binding.recycler.setHasFixedSize(true)
                            }
                        }
                        else{
                            Toast.makeText(requireContext(), "내 답변 리스트를 불러오는데에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<MyAnswerListModel>, t: Throwable) {
                        Toast.makeText(requireContext(), "내 답변 리스트를 불러오는데에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    }

                })
        }
//        val data: ArrayList<QnAData> = ArrayList()
//        if(list.isEmpty()){
//            data.add(QnAData("오늘 점심은 뭐 먹죠?", "점심을 먹죠 ㅎㅎ 아 점심을 먹는다니 참 감미로운 일이네요."))
//            data.add(QnAData("오늘 저녁은 뭐 먹죠?", "저녁을 먹죠 ㅎㅎ"))
//            list.add(DateClass("3월 16일", data))
//            list.add(DateClass("4월 16일", data))
//            Log.d(TAG, "onActivityCreated: 내 리스트 data$data $list")
//        }
    }

    private fun processingData(result: MyAnswerListModel?){

        for( i in 0 until result!!.size){
            result[i].createdDate = changeCreateDate(result[i].createdDate)
            Log.d(TAG, "processingData: date : ${result[i].createdDate}")
        }

        var i = 0
        while(result.size-1 >i){
            val data: ArrayList<MyAnswerListModelItem> = ArrayList()
            data.add(result[i])
            var plusI = 0
            for(j in i+1 until result.size){
                if(result[j].createdDate == result[i].createdDate){
                    data.add(result[j])
                    plusI++
                    if(j == result.size-1){
                        list.add(DateClass(result[i].createdDate,data))
                    }
                }
                else{
                    list.add(DateClass(result[i].createdDate,data))
                    break
                }
            }
            i += plusI+1
        }
        Log.d(TAG, "processingData: list : ${list}")
    }

    @SuppressLint("SimpleDateFormat")
    private fun changeCreateDate(createdDate: String) : String {
        var createSf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ss'Z'")
        var changeSf = SimpleDateFormat("MM'월' dd일")
        var date = createSf.parse(createdDate)
        
        return changeSf.format(date)
    }

    private fun setUi() {

        val adapter: Main2CalendarAdapter = Main2CalendarAdapter(calendarViewModel)
        binding.main2CalendarRecyclerView.adapter = adapter
        calendarViewModel.today = calendarHelper.getDate()

        setCalendar()
    }

    private fun setCalendar() {
        calendarViewModel.dateList = ArrayList()
        val lastEndDay: Int
        var postArray : List<String>? = null
        if (writePostMap != null)
            postArray = writePostMap!!.keys.toList()

        when (calendarHelper.getMonth() + 1) {
            1 -> {
                viewModel.month.value = "January"
                lastEndDay = 31
            }
            2 -> {
                viewModel.month.value = "February"
                lastEndDay = 31
            }
            3 -> {
                viewModel.month.value = "March"
                if (calendarHelper.getYear() % 4 == 0 && calendarHelper.getYear() % 100 != 0
                    || calendarHelper.getYear() % 400 == 0
                ) {
                    lastEndDay = 29
                } else
                    lastEndDay = 28
            }
            4 -> {
                viewModel.month.value = "April"
                lastEndDay = 31
            }
            5 -> {
                viewModel.month.value = "May"
                lastEndDay = 30
            }
            6 -> {
                viewModel.month.value = "June"
                lastEndDay = 31
            }
            7 -> {
                viewModel.month.value = "July"
                lastEndDay = 30
            }
            8 -> {
                viewModel.month.value = "August"
                lastEndDay = 31
            }
            9 -> {
                viewModel.month.value = "September"
                lastEndDay = 31
            }
            10 -> {
                viewModel.month.value = "October"
                lastEndDay = 30
            }
            11 -> {
                viewModel.month.value = "November"
                lastEndDay = 31
            }
            12 -> {
                viewModel.month.value = "December"
                lastEndDay = 30
            }
            else -> {
                viewModel.month.value = "error"
                lastEndDay = 30
            }
        }

        for (i in calendarHelper.getStartDayOfWeek() - 2 downTo 0) {

            calendarViewModel.dateList.add(

                Main2CalendarData(
                    (lastEndDay - i).toString(),
                    DataType.NONE_MOOD,
                    DataType.LAST_DAY
                )
            )
        }

        var run = false
        for (j in 0 until calendarHelper.getEndDay()) {
            run = false
            if (writePostMap != null) {
                for (k in 0 until writePostMap!!.size) {
                    Log.e("test", postArray!!.get(k).split(".").get(0))
                    if (postArray.get(k).split(".").get(0).equals(calendarHelper.getYear().toString())
                            && postArray.get(k).split(".").get(1).equals((calendarHelper.getMonth() + 1).toString())
                        && postArray[k].split(".")[2].equals((j + 1).toString())
                    ) {

                        calendarViewModel.dateList.add(
                            Main2CalendarData(
                                (j + 1).toString(),
                                writePostMap!![postArray[k]].toString().toInt(),
                                DataType.CURRENT_DAY
                            )
                        )
                        run = true
                        break
                    }
                }
                if (!run) {
                    calendarViewModel.dateList.add(
                        Main2CalendarData(
                            (j + 1).toString(),
                            DataType.NONE_MOOD,
                            DataType.CURRENT_DAY
                        )
                    )
                }

            } else {
                calendarViewModel.dateList.add(
                    Main2CalendarData(
                        (j + 1).toString(),
                        DataType.NONE_MOOD,
                        DataType.CURRENT_DAY
                    )
                )
            }
        }


        for (k in 1..7 - calendarHelper.getEndDayOfWeek()) {
            calendarViewModel.dateList.add(
                Main2CalendarData(k.toString(), DataType.NONE_MOOD, DataType.LAST_DAY)
            )
        }

        binding.main2CalendarRecyclerView.adapter!!.notifyDataSetChanged()
    }

    public fun plusMonth(view: View) {
        calendarHelper.plusMonth()

        if (calendarHelper.getYear().toString() != binding.year)
            binding.year = calendarHelper.getYear().toString()

        setCalendar()
    }

    public fun minusMonth(view: View) {
        calendarHelper.minusMonth()
        Log.e(
            "year,month",
            calendarHelper.getYear().toString() + ":  " + calendarHelper.getMonth().toString()
        )

        setCalendar()
    }


}