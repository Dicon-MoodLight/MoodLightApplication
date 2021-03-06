package com.example.moodlight.screen.main1.repository

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.moodlight.api.ServerClient
import com.example.moodlight.model.AnswerItemModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommunityRepository {
    val _todayQuestion = MutableLiveData<String?>()
    val questionId = MutableLiveData<String?>()
    val _answerList = MutableLiveData<ArrayList<AnswerItemModel?>>(ArrayList())

    var lastId = "-1"
    private val client = ServerClient.getApiService()

    suspend fun getQuestion(date: String, mood: String) {
        client.getQuestion(date, mood).let {
            if(it.isSuccessful){
                if(it.body()!!.isNotEmpty()){
                    _todayQuestion.value = it.body()!![0].contents
                    questionId.value = it.body()!![0].id
                }else{
                    _todayQuestion.value = null
                }
            }
        }
    }

    fun getAnswer() {
        client.getAnswer(ServerClient.accessToken, questionId.value, lastId)
            .enqueue(object : Callback<List<AnswerItemModel?>> {
                override fun onResponse(
                    call: Call<List<AnswerItemModel?>>,
                    response: Response<List<AnswerItemModel?>>
                ) {
                    if (response.isSuccessful) {
                        _answerList.value = response.body() as ArrayList<AnswerItemModel?>?
                        if(response.body()!!.isNotEmpty()){
                            lastId = response.body()!![(response.body() as ArrayList<AnswerItemModel?>).lastIndex]!!.id.toString()
                        }
                    }
                }

                override fun onFailure(call: Call<List<AnswerItemModel?>>, t: Throwable) {
                }
            })
    }

    fun refresh(id : String){
        lastId = id
    }
}