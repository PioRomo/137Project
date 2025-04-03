package com.example.pixelpedia
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class GameAdapter(
    private val games: List<Game>,
    private val onClick: (Game) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gameImage: ImageView = view.findViewById(R.id.gameImageView)
        val gameName: TextView = view.findViewById(R.id.gameNameTextView)
        val gameDescription: TextView = view.findViewById(R.id.gameDescriptionTextView)
        val gameGenreandConsole: TextView = view.findViewById(R.id.gameOtherTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]
        holder.gameName.text = game.gamename
        holder.gameDescription.text = game.gamedescription
        holder.gameGenreandConsole.text = "Genre: ${game.gamegenre}  |  Console: ${game.gameconsole}"

        Glide.with(holder.itemView.context)
            .load(game.gameimage)
            .placeholder(R.drawable.placeholder_game_image) // Ensure you have a placeholder image
            .into(holder.gameImage)

        holder.itemView.setOnClickListener { onClick(game) }
    }

    override fun getItemCount() = games.size
}

