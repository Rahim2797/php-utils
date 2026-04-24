<?php

use Rahim2797\MagicTypes\Properties;

function fallback(): array
{
    return [];
}

/** @var Properties<\App\Models\User> $props */
$props = rand(0, 1)
    ? fallback()
    : [
        '<caret>' => true,
    ];
