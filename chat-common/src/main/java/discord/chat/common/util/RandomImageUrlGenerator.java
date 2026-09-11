package discord.chat.common.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomImageUrlGenerator {
    private static final List<String> IMAGE_URLS = List.of(
        "https://i.pinimg.com/originals/a5/98/73/a598732adbce5c5f5c276474a5525330.jpg",
        "https://i.pinimg.com/474x/25/0f/2d/250f2d083b89d3b6585f67729602daaf.jpg",
        "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ3Vd_kqZn53ok20t0tVuAukGAHOzVLWvNgKw&s",
        "https://i.pinimg.com/564x/31/f1/23/31f1231af69beb6617062dbf3373131c.jpg"
    );

    private RandomImageUrlGenerator() {
    }

    public static String generate() {
        int imageIndex = ThreadLocalRandom.current().nextInt(IMAGE_URLS.size());
        return IMAGE_URLS.get(imageIndex);
    }
}
