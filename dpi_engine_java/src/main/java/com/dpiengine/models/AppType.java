package com.dpiengine.models;

import java.util.Locale;

public enum AppType {
    UNKNOWN,
    HTTP,
    HTTPS,
    DNS,
    TLS,
    QUIC,
    GOOGLE,
    FACEBOOK,
    YOUTUBE,
    TWITTER,
    INSTAGRAM,
    NETFLIX,
    AMAZON,
    MICROSOFT,
    APPLE,
    WHATSAPP,
    TELEGRAM,
    TIKTOK,
    SPOTIFY,
    ZOOM,
    DISCORD,
    GITHUB,
    CLOUDFLARE,
    APP_COUNT;

    @Override
    public String toString() {
        switch (this) {
            case UNKNOWN: return "Unknown";
            case HTTP: return "HTTP";
            case HTTPS: return "HTTPS";
            case DNS: return "DNS";
            case TLS: return "TLS";
            case QUIC: return "QUIC";
            case GOOGLE: return "Google";
            case FACEBOOK: return "Facebook";
            case YOUTUBE: return "YouTube";
            case TWITTER: return "Twitter/X";
            case INSTAGRAM: return "Instagram";
            case NETFLIX: return "Netflix";
            case AMAZON: return "Amazon";
            case MICROSOFT: return "Microsoft";
            case APPLE: return "Apple";
            case WHATSAPP: return "WhatsApp";
            case TELEGRAM: return "Telegram";
            case TIKTOK: return "TikTok";
            case SPOTIFY: return "Spotify";
            case ZOOM: return "Zoom";
            case DISCORD: return "Discord";
            case GITHUB: return "GitHub";
            case CLOUDFLARE: return "Cloudflare";
            default: return "Unknown";
        }
    }

    public static AppType fromSni(String sni) {
        if (sni == null || sni.isEmpty()) return UNKNOWN;
        
        String lowerSni = sni.toLowerCase(Locale.ROOT);
        
        if (lowerSni.contains("google") || lowerSni.contains("gstatic") || lowerSni.contains("googleapis") || lowerSni.contains("ggpht") || lowerSni.contains("gvt1")) {
            return GOOGLE;
        }
        if (lowerSni.contains("youtube") || lowerSni.contains("ytimg") || lowerSni.contains("youtu.be") || lowerSni.contains("yt3.ggpht")) {
            return YOUTUBE;
        }
        if (lowerSni.contains("facebook") || lowerSni.contains("fbcdn") || lowerSni.contains("fb.com") || lowerSni.contains("fbsbx") || lowerSni.contains("meta.com")) {
            return FACEBOOK;
        }
        if (lowerSni.contains("instagram") || lowerSni.contains("cdninstagram")) {
            return INSTAGRAM;
        }
        if (lowerSni.contains("whatsapp") || lowerSni.contains("wa.me")) {
            return WHATSAPP;
        }
        if (lowerSni.contains("twitter") || lowerSni.contains("twimg") || lowerSni.contains("x.com") || lowerSni.contains("t.co")) {
            return TWITTER;
        }
        if (lowerSni.contains("netflix") || lowerSni.contains("nflxvideo") || lowerSni.contains("nflximg")) {
            return NETFLIX;
        }
        if (lowerSni.contains("amazon") || lowerSni.contains("amazonaws") || lowerSni.contains("cloudfront") || lowerSni.contains("aws")) {
            return AMAZON;
        }
        if (lowerSni.contains("microsoft") || lowerSni.contains("msn.com") || lowerSni.contains("office") || lowerSni.contains("azure") || lowerSni.contains("live.com") || lowerSni.contains("outlook") || lowerSni.contains("bing")) {
            return MICROSOFT;
        }
        if (lowerSni.contains("apple") || lowerSni.contains("icloud") || lowerSni.contains("mzstatic") || lowerSni.contains("itunes")) {
            return APPLE;
        }
        if (lowerSni.contains("telegram") || lowerSni.contains("t.me")) {
            return TELEGRAM;
        }
        if (lowerSni.contains("tiktok") || lowerSni.contains("tiktokcdn") || lowerSni.contains("musical.ly") || lowerSni.contains("bytedance")) {
            return TIKTOK;
        }
        if (lowerSni.contains("spotify") || lowerSni.contains("scdn.co")) {
            return SPOTIFY;
        }
        if (lowerSni.contains("zoom")) {
            return ZOOM;
        }
        if (lowerSni.contains("discord") || lowerSni.contains("discordapp")) {
            return DISCORD;
        }
        if (lowerSni.contains("github") || lowerSni.contains("githubusercontent")) {
            return GITHUB;
        }
        if (lowerSni.contains("cloudflare") || lowerSni.contains("cf-")) {
            return CLOUDFLARE;
        }
        
        return HTTPS;
    }
}
