package com.alimrasid.prototype1.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alimrasid.prototype1.R

import android.content.Intent
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alimrasid.prototype1.ui.home.alphabet.AlphabetActivity
import com.alimrasid.prototype1.ui.home.news.NewsAdapter
import com.alimrasid.prototype1.ui.home.word.WordActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var rvNews: RecyclerView
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var linearLayout: LinearLayout
    private lateinit var tvError: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(HomeViewModel::class.java)

        val user = FirebaseAuth.getInstance().currentUser
        val tvGreeting: TextView = view.findViewById(R.id.tvGreeting)
        val btnWord: Button = view.findViewById(R.id.btnWord)
        val btnAlphabet: Button = view.findViewById(R.id.btnAlphabet)
        val imgProfile: ImageView = view.findViewById(R.id.imgProfile)
        tvError = view.findViewById(R.id.tvError)
        linearLayout = view.findViewById(R.id.emptyState)
        linearLayout.visibility = View.GONE

        btnAlphabet.setOnClickListener {
            startActivity(Intent(requireContext(), AlphabetActivity::class.java))
        }
        btnWord.setOnClickListener {
            startActivity(Intent(requireContext(), WordActivity::class.java))
        }

        user?.let {
            tvGreeting.text = getString(R.string.greeting, it.displayName)
            Glide.with(requireContext())
                .load(it.photoUrl)
                .optionalCircleCrop()
                .into(imgProfile)
        }

        rvNews = view.findViewById(R.id.rvBerita)
        progressBar = view.findViewById(R.id.progressBar)
        newsAdapter = NewsAdapter()

        observeConnectionState()
    }

    private fun observeNews() {
        viewModel.getNews().observe(viewLifecycleOwner) { newsList ->
            newsAdapter.setNews(newsList)
            rvNews.layoutManager = LinearLayoutManager(requireContext())
            rvNews.setHasFixedSize(true)
            rvNews.adapter = newsAdapter
        }
    }

    private fun observeLoading() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun observeConnectionState() {
        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                linearLayout.visibility = View.GONE
                rvNews.visibility = View.VISIBLE
            } else {
                linearLayout.visibility = View.VISIBLE
                rvNews.visibility = View.GONE
                tvError.text = "Tidak ada koneksi internet!"
            }
            observeLoading()
            observeNews()
        }
    }
}
