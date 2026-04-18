package com.example.gamelog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;

public class HomeTabFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home_tab, container, false);

        MaterialCardView collectionCard = root.findViewById(R.id.home_collection_card);
        collectionCard.setOnClickListener(v -> startActivity(new Intent(requireContext(), CollectionActivity.class)));

        MaterialCardView favouritesCard = root.findViewById(R.id.home_favourites_card);
        favouritesCard.setOnClickListener(v -> startActivity(new Intent(requireContext(), FavouritesActivity.class)));

        MaterialCardView exploreCard = root.findViewById(R.id.home_explore_card);
        exploreCard.setOnClickListener(v -> startActivity(new Intent(requireContext(), ApiGamesActivity.class)));

        return root;
    }
}
