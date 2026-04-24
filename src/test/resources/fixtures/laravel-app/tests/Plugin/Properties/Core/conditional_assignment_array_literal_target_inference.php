<?php

use Rahim2797\MagicTypes\Properties;

function fallback(): array
{
    return [];
}

/** @var Properties<\App\Models\User> $data */
$data = rand(0, 1)
    ? fallback()
    : ['email' => 'Ada'];
