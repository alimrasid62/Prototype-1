package com.alimrasid.prototype1.ui.home.news

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.alimrasid.prototype1.R
import com.alimrasid.prototype1.data.News
import com.bumptech.glide.Glide

class NewsAdapter : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private var newsList: List<News>? = null

    fun setNews(list: List<News>) {
        if (newsList == null) {
            newsList = list
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.news_item, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList?.get(position)
        val context = holder.itemView.context

        holder.tvNewsTitle.text = news?.title
        holder.tvNewsDesc.text = news?.content
        holder.tvType.text = news?.type
        if (news?.type == "news") {
            holder.tvType.setBackgroundResource(R.drawable.news_badge)
        } else {
            holder.tvType.setBackgroundResource(R.drawable.campaign_badge)
        }
        Glide.with(context)
            .load(news?.imgUrl)
            .into(holder.imgNews)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, NewsWebViewActivity::class.java)
            intent.putExtra(NewsWebViewActivity.URL_EXTRA, news?.source)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return newsList?.size ?: 0
    }

    class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgNews: ImageView = itemView.findViewById(R.id.imgNews)
        val tvNewsTitle: TextView = itemView.findViewById(R.id.tvNewsTitle)
        val tvNewsDesc: TextView = itemView.findViewById(R.id.tvNewsDesc)
        val tvType: TextView = itemView.findViewById(R.id.tvType)
    }
}
