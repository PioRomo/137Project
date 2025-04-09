package com.example.pixelpedia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserProfileAdapter(
    private val profiles: List<UserProfile>,
    private val onClick: (UserProfile) -> Unit
) : RecyclerView.Adapter<UserProfileAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: ImageView = itemView.findViewById(R.id.itemProfileImage)
        val nameText: TextView = itemView.findViewById(R.id.itemProfileName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_profile, parent, false)
        return ProfileViewHolder(view)
    }

    override fun getItemCount(): Int = profiles.size

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val profile = profiles[position]
        holder.nameText.text = profile.username

        Glide.with(holder.itemView.context)
            .load(profile.profilePicUrl)
            .placeholder(R.drawable.placeholder_game_image)
            .circleCrop()
            .into(holder.profilePic)

        holder.itemView.setOnClickListener { onClick(profile) }
    }
}
