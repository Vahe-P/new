package com.example.anew;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<DocumentSnapshot> comments;
    private Context context;
    private String placeId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public CommentAdapter(Context context, List<DocumentSnapshot> comments, String placeId) {
        this.context = context;
        this.comments = comments;
        this.placeId = placeId;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        DocumentSnapshot comment = comments.get(position);
        
        String userId = comment.getString("userId");
        String userName = comment.getString("userName");
        String text = comment.getString("text");
        long timestamp = comment.getLong("timestamp");
        Map<String, Boolean> likes = (Map<String, Boolean>) comment.get("likes");
        if (likes == null) {
            likes = new HashMap<>();
        }

        holder.userName.setText(userName);
        holder.commentText.setText(text);
        
        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String timeString = sdf.format(new Date(timestamp));
        holder.commentTime.setText(timeString);

        // Load user's profile picture from Firestore
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String profilePictureUrl = userDoc.getString("profilePictureUrl");
                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            Glide.with(context)
                                .load(profilePictureUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .circleCrop()
                                .into(holder.userAvatar);
                        } else {
                            holder.userAvatar.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        holder.userAvatar.setImageResource(R.drawable.default_avatar);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CommentAdapter", "Error loading profile picture", e);
                    holder.userAvatar.setImageResource(R.drawable.default_avatar);
                });
        } else {
            holder.userAvatar.setImageResource(R.drawable.default_avatar);
        }

        // Show edit/delete buttons only for the comment owner
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId != null && currentUserId.equals(userId)) {
            holder.editButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.editButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);
        }

        // Update like button state and count
        boolean isLiked = currentUserId != null && likes.containsKey(currentUserId) && likes.get(currentUserId);
        holder.likeButton.setImageResource(isLiked ? R.drawable.baseline_thumb_up_filled_24 : R.drawable.baseline_thumb_up_24);
        holder.likeCount.setText(String.valueOf(likes.size()));

        // Set up click listeners
        holder.editButton.setOnClickListener(v -> showEditDialog(comment));
        holder.deleteButton.setOnClickListener(v -> showDeleteDialog(comment));
        holder.likeButton.setOnClickListener(v -> handleLike(comment, holder));
    }

    private void showEditDialog(DocumentSnapshot comment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_comment, null);
        EditText editText = view.findViewById(R.id.editCommentText);
        editText.setText(comment.getString("text"));
        
        AlertDialog dialog = builder.setView(view)
                .setTitle("Edit Comment")
                .setPositiveButton("Save", null) // Set to null initially
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String newText = editText.getText().toString().trim();
                if (!newText.isEmpty()) {
                    updateComment(comment.getId(), newText);
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showDeleteDialog(DocumentSnapshot comment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteComment(comment.getId());
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateComment(String commentId, String newText) {
        db.collection("places").document(placeId)
                .collection("comments").document(commentId)
                .update("text", newText)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Comment updated", Toast.LENGTH_SHORT).show();
                    // Refresh the comments list
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteComment(String commentId) {
        db.collection("places").document(placeId)
                .collection("comments").document(commentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show();
                    // Remove the comment from the local list
                    comments.removeIf(comment -> comment.getId().equals(commentId));
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleLike(DocumentSnapshot comment, CommentViewHolder holder) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Please sign in to like comments", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = currentUser.getUid();

        // Get the current likes map from the comment
        Map<String, Boolean> likes = (Map<String, Boolean>) comment.get("likes");
        if (likes == null) {
            likes = new HashMap<>();
        }

        // Toggle the like state
        boolean isLiked = likes.containsKey(currentUserId) && Boolean.TRUE.equals(likes.get(currentUserId));
        likes.put(currentUserId, !isLiked);  // toggle like

        // Final version of the map to use in lambda
        final Map<String, Boolean> finalLikes = new HashMap<>(likes);
        final boolean newLikeState = !isLiked;

        // Make sure placeId and commentId are valid
        String commentId = comment.getId();
        if (placeId == null || commentId == null) {
            Toast.makeText(context, "Invalid comment or place ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Perform the update
        FirebaseFirestore.getInstance()
                .collection("places").document(placeId)
                .collection("comments").document(commentId)
                .update("likes", finalLikes)
                .addOnSuccessListener(aVoid -> {
                    // Update UI
                    holder.likeButton.setImageResource(newLikeState
                            ? R.drawable.baseline_thumb_up_filled_24
                            : R.drawable.baseline_thumb_up_24);
                    holder.likeCount.setText(String.valueOf(finalLikes.size()));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update like: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("FirestoreLike", "Error updating like", e);
                });
    }


    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void updateComments(List<DocumentSnapshot> newComments) {
        this.comments = newComments;
        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userAvatar;
        TextView userName, commentText, commentTime, likeCount;
        ImageButton editButton, deleteButton, likeButton;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            commentText = itemView.findViewById(R.id.commentText);
            commentTime = itemView.findViewById(R.id.commentTime);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            likeButton = itemView.findViewById(R.id.likeButton);
            likeCount = itemView.findViewById(R.id.likeCount);
        }
    }
} 