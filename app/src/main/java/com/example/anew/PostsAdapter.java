package com.example.anew;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {
    private Context context;
    private java.util.List<Post> posts;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SimpleDateFormat dateFormat;

    public PostsAdapter(Context context, java.util.List<Post> posts) {
        this.context = context;
        this.posts = posts;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        
        // Set user info
        holder.userName.setText(post.getUserName());
        Glide.with(context)
            .load(post.getUserAvatar())
            .placeholder(R.drawable.default_avatar)
            .into(holder.userAvatar);

        // Set post content
        holder.postContent.setText(post.getContent());
        
        // Set post image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.postImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(post.getImageUrl())
                .into(holder.postImage);
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        // Set location
        if (post.getLocation() != null) {
            holder.locationContainer.setVisibility(View.VISIBLE);
            holder.locationText.setText(post.getLocationName());
        } else {
            holder.locationContainer.setVisibility(View.GONE);
        }

        // Set timestamp
        holder.postTime.setText(formatTimestamp(post.getTimestamp()));

        // Set like count and state
        holder.likeCount.setText(String.valueOf(post.getLikes().size()));
        holder.btnLike.setImageResource(post.isLiked() ? 
            R.drawable.baseline_favorite_24 : R.drawable.baseline_favorite_border_24);

        // Set comment count
        holder.commentCount.setText(String.valueOf(post.getComments().size()));

        // Set up comments RecyclerView
        CommentsAdapter commentsAdapter = new CommentsAdapter(context, post.getComments());
        holder.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        holder.commentsRecyclerView.setAdapter(commentsAdapter);

        // Set up click listeners
        holder.btnLike.setOnClickListener(v -> toggleLike(post, holder));
        holder.btnComment.setOnClickListener(v -> toggleComments(holder));
        holder.btnShare.setOnClickListener(v -> sharePost(post));
        holder.btnViewMap.setOnClickListener(v -> viewOnMap(post));
        holder.btnMore.setOnClickListener(v -> showMoreOptions(post));
        holder.btnSendComment.setOnClickListener(v -> addComment(post, holder));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    private void toggleLike(Post post, PostViewHolder holder) {
        String userId = auth.getCurrentUser().getUid();
        if (post.isLiked()) {
            post.getLikes().remove(userId);
            post.setLiked(false);
        } else {
            post.getLikes().add(userId);
            post.setLiked(true);
        }
        
        db.collection("posts").document(post.getId())
            .update("likes", post.getLikes())
            .addOnSuccessListener(aVoid -> {
                holder.likeCount.setText(String.valueOf(post.getLikes().size()));
                holder.btnLike.setImageResource(post.isLiked() ? 
                    R.drawable.baseline_favorite_24 : R.drawable.baseline_favorite_border_24);
            })
            .addOnFailureListener(e -> 
                Toast.makeText(context, "Error updating like", Toast.LENGTH_SHORT).show());
    }

    private void toggleComments(PostViewHolder holder) {
        boolean isVisible = holder.commentsContainer.getVisibility() == View.VISIBLE;
        holder.commentsContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    private void sharePost(Post post) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = post.getContent() + "\n\nPosted by " + post.getUserName();
        if (post.getLocationName() != null) {
            shareText += "\nLocation: " + post.getLocationName();
        }
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        context.startActivity(Intent.createChooser(shareIntent, "Share post"));
    }

    private void viewOnMap(Post post) {
        if (post.getLocation() != null) {
            Intent intent = new Intent(context, MapActivity.class);
            intent.putExtra("userLat", post.getLocation().getLatitude());
            intent.putExtra("userLng", post.getLocation().getLongitude());
            context.startActivity(intent);
        }
    }

    private void showMoreOptions(Post post) {
        // TODO: Implement more options menu (report, delete if own post, etc.)
    }

    private void addComment(Post post, PostViewHolder holder) {
        String commentText = holder.commentInput.getText().toString().trim();
        if (!commentText.isEmpty()) {
            String userId = auth.getCurrentUser().getUid();
            String userName = auth.getCurrentUser().getDisplayName();
            String userAvatar = auth.getCurrentUser().getPhotoUrl() != null ? 
                auth.getCurrentUser().getPhotoUrl().toString() : null;

            Post.Comment comment = new Post.Comment(userId, userName, userAvatar, commentText);
            post.getComments().add(comment);

            db.collection("posts").document(post.getId())
                .update("comments", post.getComments())
                .addOnSuccessListener(aVoid -> {
                    holder.commentInput.setText("");
                    holder.commentCount.setText(String.valueOf(post.getComments().size()));
                    holder.commentsRecyclerView.getAdapter().notifyDataSetChanged();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(context, "Error adding comment", Toast.LENGTH_SHORT).show());
        }
    }

    private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        if (timestamp != null) {
            return dateFormat.format(timestamp.toDate());
        }
        return "";
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userAvatar;
        TextView userName;
        TextView postTime;
        ImageView postImage;
        TextView postContent;
        LinearLayout locationContainer;
        TextView locationText;
        ImageButton btnLike;
        TextView likeCount;
        ImageButton btnComment;
        TextView commentCount;
        ImageButton btnShare;
        ImageButton btnViewMap;
        ImageButton btnMore;
        LinearLayout commentsContainer;
        RecyclerView commentsRecyclerView;
        EditText commentInput;
        ImageButton btnSendComment;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            postTime = itemView.findViewById(R.id.postTime);
            postImage = itemView.findViewById(R.id.postImage);
            postContent = itemView.findViewById(R.id.postContent);
            locationContainer = itemView.findViewById(R.id.locationContainer);
            locationText = itemView.findViewById(R.id.locationText);
            btnLike = itemView.findViewById(R.id.btnLike);
            likeCount = itemView.findViewById(R.id.likeCount);
            btnComment = itemView.findViewById(R.id.btnComment);
            commentCount = itemView.findViewById(R.id.commentCount);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnViewMap = itemView.findViewById(R.id.btnViewMap);
            btnMore = itemView.findViewById(R.id.btnMore);
            commentsContainer = itemView.findViewById(R.id.commentsContainer);
            commentsRecyclerView = itemView.findViewById(R.id.commentsRecyclerView);
            commentInput = itemView.findViewById(R.id.commentInput);
            btnSendComment = itemView.findViewById(R.id.btnSendComment);
        }
    }
} 