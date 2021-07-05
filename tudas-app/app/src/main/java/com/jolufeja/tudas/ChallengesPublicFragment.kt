package com.jolufeja.tudas

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import arrow.core.Either
import com.jolufeja.httpclient.HttpClient
import com.jolufeja.httpclient.error.CommonErrors
import com.jolufeja.httpclient.error.ErrorHandler
import com.jolufeja.httpclient.error.HttpErrorHandler
import com.jolufeja.presentation.viewmodel.DataSource
import com.jolufeja.presentation.viewmodel.FetcherViewModel
import com.jolufeja.tudas.adapters.RecycleViewAdapter
import com.jolufeja.tudas.data.ChallengesItem
import com.jolufeja.tudas.data.HeaderItem
import com.jolufeja.tudas.data.ListItem
import com.jolufeja.tudas.service.challenges.Challenge
import com.jolufeja.tudas.service.challenges.ChallengeService
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private val ChallengeErrorHandler = ErrorHandler(CommonErrors::GenericError)


class ChallengesPublicViewModel(
    private val challengeService: ChallengeService
) : FetcherViewModel<CommonErrors, List<Challenge>>(ChallengeErrorHandler) {
    override suspend fun fetchData(): List<Challenge> =
        when(val challenges = challengeService.getPublicChallenges()) {
            is Either.Right -> challenges.value
            is Either.Left -> throw Throwable("Unable to fetch public challenges ${challenges.value}")
        }

}

class ChallengesPublicFragment : Fragment(R.layout.fragment_challenges_public) {
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var listOfChallenges: ArrayList<ChallengesItem> = ArrayList()
    private var createChallengeButton: Button? = null
    private var finalList: MutableList<ListItem> = ArrayList()

    private val viewModel: ChallengesPublicViewModel by inject()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Need to figure out a way to integrate asynchronous loading into all of this ... :/
        lifecycleScope.launchWhenCreated {
            viewModel.fetchStatus.collect {
                when(val state = it) {
                    is DataSource.State.Empty -> {}
                    is DataSource.State.Error -> {}
                    is DataSource.State.Loading -> {
                        // TODO: Display loading animation
                    }
                    is DataSource.State.Success -> {
                        val newItems = state.data.toChallengeListItems()
                        finalList = newItems.toMutableList()
                        mAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }


        //adding items in list
        for (i in 0..10) {
            val challenges = ChallengesItem()
            challenges.id = i
            challenges.title = "Challenge $i"
            challenges.author = "Max Mustermann"
            challenges.description =
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum sagittis at justo a volutpat. In sed leo vel ipsum egestas mattis vitae eget lorem."
            challenges.points = 100
            challenges.timeLeft = 200
            listOfChallenges.add(challenges)
        }

        val header = HeaderItem()
        header.text = "Open"
        finalList.add(header)

        listOfChallenges.forEach {
            finalList.add(it)
        }

        val header1 = HeaderItem()
        header1.text = "Completed"
        finalList.add(header1)

        listOfChallenges.forEach {
            finalList.add(it)
        }

        mRecyclerView = view.findViewById(R.id.challenges_public_recycler_view)
        var mLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mRecyclerView!!.layoutManager = mLayoutManager
        // Add Adapter so cards will be displayed
        mAdapter = context?.let {
            RecycleViewAdapter(
                it,
                finalList,
                R.layout.card_challenges_public,
                R.layout.card_header,
                0,
                0,
                0,
                0
            ) { item ->
                // Open New Fragment
                val individualChallengePublicFragment = IndividualChallengePublicFragment()
                val transaction: FragmentTransaction =
                    requireActivity().supportFragmentManager.beginTransaction()
                transaction.replace(
                    ((view as ViewGroup).parent as View).id,
                    individualChallengePublicFragment
                )
                transaction.addToBackStack("challenge_sent_info")
                transaction.commit()
                item.id.let { Log.d("TAG", it.toString()) }
            }
        }
        mRecyclerView!!.adapter = mAdapter

        // Handle Create Challenge Button
        createChallengeButton = view.findViewById(R.id.create_challenge_button) as Button
        createChallengeButton!!.setOnClickListener {
            // Open New Fragment
            val individualChallengePublicFragment = IndividualChallengePublicFragment()
            val transaction: FragmentTransaction =
                requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(
                ((view as ViewGroup).parent as View).id,
                individualChallengePublicFragment
            )
            transaction.addToBackStack("challenge_sent_info")
            transaction.commit()
        }
    }
}

private fun  List<Challenge>.toChallengeListItems(): List<ListItem> = mapIndexed { i, challenge ->
    val item = ListItem()

    item.id = i
    item.title = challenge.challengeName
    item.author = challenge.creatorName
    item.description = challenge.description
    item.points = challenge.points
    item.timeLeft = LocalTime.now().until(challenge.dueDate, ChronoUnit.DAYS).toInt()

    item
}
